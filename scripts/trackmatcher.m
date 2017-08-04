%% Matches two TrackMate tracks based on their spot-spot distance.
% The tracks of the two files are loaded as list of spots. 

close all
clear
clc

% Calcium TrackMate XML file.
fileCalcium = '/Users/tinevez/Google Drive/Projects/Contacts/raw data/2015-09-17/Trackmate files/SiC - SAg_1_20_Calcium.xml';

% Contact TrackMate XML file.
fileContacts = '/Users/tinevez/Google Drive/Projects/Contacts/raw data/2015-09-17/Trackmate files/SiC - SAg_1_20_Contacts.xml';

% Minimal number of edges in contact tracks below which tracks are not
% considered.
minNEdges = 10;

% Maximal mean distance between two tracks to accept a match.
maxTrackDist = 20; % physical units

% Plot tracks?
doPlot = true;

% Load colormap
load mycmap

%% Get calibration.

cal = trackmateImageCalibration(fileCalcium);
nFrames = cal.t.size;
fprintf('Found %d frames in source image.\n', nFrames)

%% Import calcium tracks.

fprintf('Loading Calcium XML file...\n')

[ tracksCalcium, tracksCalciumNames ] = loadtracks( fileCalcium, ...
    { 'POSITION_X', 'POSITION_Y', 'FRAME', 'MEAN_INTENSITY' } );

fprintf('Done.\n')

%% Filter calcium tracks.

nEdgesTracksCalcium = cellfun(@(x) size(x, 1), tracksCalcium );
tracksCalcium( nEdgesTracksCalcium < minNEdges ) = [];
tracksCalciumNames( nEdgesTracksCalcium < minNEdges ) = [];

fprintf('Retaining %d tracks out of %d.\n', numel( tracksCalcium ), numel( nEdgesTracksCalcium ) )

%% Import contact tracks.

fprintf('Loading Contact XML file...\n')

[ tracksContacts, tracksContactsNames ] = loadtracks( fileContacts, ...
    { 'POSITION_X', 'POSITION_Y', 'FRAME', 'QUALITY' } );

fprintf('Done.\n')

%% Filter contact tracks.

nEdgesTracksContacts = cellfun(@(x) size(x, 1), tracksContacts );
tracksContacts( nEdgesTracksContacts < minNEdges ) = [];
tracksContactsNames( nEdgesTracksContacts < minNEdges ) = [];

fprintf('Retaining %d tracks out of %d.\n', numel( tracksContacts ), numel( nEdgesTracksContacts ) )

%% Match.
fprintf('Matching contact tracks to calcium tracks.\n')

ntcalcium = numel(tracksCalcium);
ntcontacts = numel(tracksContacts);
matches = [];

if doPlot
    colors = 0.8 * hsv( ntcontacts );
    hf1 = figure('Position', [680   200   900   750]);
    hold on
end

for i = 1 : ntcontacts 

    fprintf('\nContact track %d of %d.\n', i, ntcontacts )
    
    track_2 = tracksContacts{ i };
    frames_2 = track_2.FRAME + 1;
    pos_2 = [ track_2.POSITION_X track_2.POSITION_Y ];
    
    dpos2 = NaN(nFrames, 2);
    dpos2( frames_2, : ) = pos_2;
    
    distMean = NaN(ntcalcium, 1);
    distStd = NaN(ntcalcium, 1);
    distN = NaN(ntcalcium, 1);
    
    for j = 1 : ntcalcium
        
        track_1 = tracksCalcium{ j };
        frames_1 = track_1.FRAME + 1;
        pos_1 = [ track_1.POSITION_X track_1.POSITION_Y ];
        
        dpos1 = NaN(nFrames, 2);
        dpos1( frames_1, : ) = pos_1;
        
        delta = dpos2 - dpos1;
        dist = sqrt( sum(delta .* delta, 2) );
        distMean(j) = nanmean(dist);
        distStd(j) = nanstd(dist);
        distN(j) = numel( dist( ~isnan(dist) ) );
        
    end
    
    [ val, target_id ] = min( distMean );
    fprintf('Matched contact track #%d with calcium track #%d,\nwith a distance of %.1f +/- %.1f %s calculated over %d spots.\n', ...
        i, target_id, val, distStd(target_id), cal.x.units, distN(target_id) )
    
    
    if val > maxTrackDist
        fprintf('Mean distance value is above max tolerance distance. Match rejected.\n')
        continue
    end
    
    matches = [
        matches ;
        i, target_id]; %#ok<AGROW>
    
    if doPlot
        plot( pos_2(:,1), pos_2(:,2), ...
            'DisplayName', [ 'Contact #' num2str(i) ], ...
            'Color', colors(i, :), ...
            'Marker', 's', ...
            'MarkerFaceColor',  colors(i, :))
        
        track_1 = tracksCalcium{ target_id };
        pos_1 = [ track_1.POSITION_X track_1.POSITION_Y ];
        
        lx = min(  [ pos_2(:,1) ; pos_1(:,1) ] );
        ux = max(  [ pos_2(:,1) ; pos_1(:,1) ] );
        ly = min(  [ pos_2(:,2) ; pos_1(:,2) ] );
        uy = max(  [ pos_2(:,2) ; pos_1(:,2) ] );
        rectangle('Position', [ lx ly (ux-lx) (uy-ly) ], ...
            'EdgeColor', colors(i, :))
        
        text( (ux+lx)/2, ly - 5, ...
            num2str( size(matches, 1) ), ...
            'HorizontalAlignment', 'center', ...
            'Color', colors(i, :), ...
            'BackgroundColor', 'w')
        
        text( ux , uy, ...
            { [' Contact: ' tracksContactsNames{ i } ]
            [' Calcium: '  tracksCalciumNames{ target_id } ] }, ...        
            'HorizontalAlignment', 'left', ...
            'VerticalAlignment', 'bottom', ...
            'Color', colors(i, :), ...
            'Interpreter', 'None')

        
        plot( pos_1(:,1), pos_1(:,2), ...
            'DisplayName', [ 'Calcium #' num2str(target_id) ], ...
            'Color', colors(i, :), ...
            'Marker', 'o', ...
            'MarkerFaceColor',  'w')
        
        
    end

end


if doPlot
    xlabel([ 'X (' cal.x.units ')' ] )
    ylabel([ 'Y (' cal.y.units ')' ] )
    set(gca, 'TickDir', 'out', ...
        'YDir', 'reverse', ...
        'XAxisLocation', 'top')
    axis equal
end


%% Plot kymographs.

nMatches = size(matches, 1);
fprintf('\nFound %d matches out of %d contact tracks.\n', nMatches, ntcontacts)

kymograph = zeros( 2 * nMatches, nFrames );

% Look for min & max
% min_int = Inf;
% max_int = 0;

% for k = 1 : n_matches
%     
%     j = matches(k, 2);
%     track_1 = tracks_calcium{ j };
%     
%     intensity_raw = track_1(:,4);
%     min_int = min( min_int, min(intensity_raw) );
%     max_int = max( max_int, max(intensity_raw) );
% end

figure
hold on

for k = 1 : nMatches
   
    i = matches(k, 1);
    j = matches(k, 2);
    
    track_2 = tracksContacts{ i };
    track_1 = tracksCalcium{ j };

    frames_quality = track_2.FRAME + 1;
    quality = track_2.QUALITY;

    frames_intensity = track_1.FRAME + 1;
    intensity_raw = track_1.MEAN_INTENSITY;
    
    % Normalize min/max.
    intensity = intensity_raw / median(intensity_raw);
%     intensity = ( intensity - min(intensity) ) ./ ( max(intensity) - min(intensity) );
    
    plot(frames_intensity, intensity)
    
    contact = -0.5 * (quality > 0.01) - 0.5 * (quality > 0.2);
    kymograph( 2*k-1,   frames_quality ) = contact;
    kymograph( 2*k,     frames_intensity ) = intensity;

end

% cmap = [ 
%     repmat( [ 0.2 0.6 0.2 ], [ 32 1 ]);
%     repmat( [ 0.2 0.2 0.6 ], [ 32 1 ]);
%     0 0 0;
%     hot(64) ];


hf2 = figure('Position', [ 680    50   700   800 ] );
colormap(cmap4)
imagesc(kymograph)
box off 

for i = 1 : nMatches
   line( [0 nFrames], 0.5 + 2*[ i i ], ...
       'Color', 'w',...
       'LineWidth', 3)
    
end

set(gca, ...
    'TickDir', 'out', ...
    'YTick', 0.5 + 1 : 2 : 2 * nMatches, ...
    'YTickLabel', 1 : nMatches, ...
    'Position', [0.1300    0.1100    0.7    0.8150 ])
xlabel('Time (frames)')
ylabel('Track matches')

% Add 2nd axis.
ax1 = gca;
ax2 = axes(...
    'Units',get(ax1,'Units'), ...
    'Position',get(ax1,'Position'), ...
    'Parent',get(ax1,'Parent'), ...
	'YAxisLocation','right', ...
    'Color','none', ...
    'XGrid','off', ...
    'Xcolor', 'none', ...
    'YGrid','off',...
    'Box','off', ...
    'TickLabelInterpreter','none');
ylim( ax2, ylim( ax1 ) );


y2labels = cell( 2* nMatches, 1);
for i = 1 : nMatches
    j = matches(i, 2);
    y2labels{ 2 * i - 1 } = [' Contact: ' tracksContactsNames{ i } ];
    y2labels{ 2 * i  } = [' Calcium: '  tracksCalciumNames{ j } ];
end

set(ax2, ...
    'TickDir', 'out', ...
    'YTick', 1 : 2 * nMatches, ...
    'YTickLabel', y2labels)

[~, name] = fileparts(fileCalcium);
title(name, ...
    'Interpreter', 'None')


%% Save figures

return

saveNameTracks = [name '-tracks.pdf' ];
saveNameMatches = [name '-matches.pdf' ];

export_fig('-r600', saveNameTracks, hf1);
export_fig('-r600', '-opengl', saveNameMatches, hf2);

