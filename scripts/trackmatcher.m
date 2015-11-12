%% Matches two TrackMate tracks based on their spot-spot distance.
% The tracks of the two files are loaded as list of spots. 

close all
clear
clc

% Calcium TrackMate XML file.
file_calcium = '/Users/tinevez/Projects/AMikhailova/Data/Synch/SiC + SAg2.xml';

% Contact TrackMate XML file.
file_contacts = '/Users/tinevez/Projects/AMikhailova/Data/Synch/SiC + SAg2 contact tracker.xml';

% Minimal number of edges in contact tracks below which tracks are not
% considered.
min_n_edges = 60;

% Maximal mean distance between two tracks to accept a match.
max_track_dist = 20; % physical units

% Plot tracks?
do_plot = true;

%% Get calibration.

cal = trackMateGetCalibration(file_calcium);
n_frames = cal.t.size;
fprintf('Found %d frames in source image.\n', n_frames)

%% Import calcium tracks.

fprintf('Loading Calcium XML file... ')

tracks_calcium = trackMateGetFrom( file_calcium, 'Tracks', ...
    { 'POSITION_X', 'POSITION_Y', 'FRAME', 'MEAN_INTENSITY' } );

fprintf(' Done.\n')

%% Filter calcium tracks.

n_edges_tracks_calcium = cellfun(@(x) size(x, 1), tracks_calcium );
tracks_calcium( n_edges_tracks_calcium < min_n_edges ) = [];

fprintf('Retaining %d tracks out of %d.\n', numel(tracks_calcium), numel(n_edges_tracks_calcium) )

%% Import contact tracks.

fprintf('Loading Contact XML file... ')


tracks_contacts = trackMateGetFrom( file_contacts, 'Tracks', ...
    { 'POSITION_X', 'POSITION_Y', 'FRAME', 'QUALITY' } );

fprintf(' Done.\n')

%% Filter contact tracks.

n_edges_tracks_contacts = cellfun(@(x) size(x, 1), tracks_contacts );
tracks_contacts( n_edges_tracks_contacts < min_n_edges ) = [];

fprintf('Retaining %d tracks out of %d.\n', numel(tracks_contacts), numel(n_edges_tracks_contacts) )

%% Match.
fprintf('Matching contact tracks to calcium tracks.\n')

ntcalcium = numel(tracks_calcium);
ntcontacts = numel(tracks_contacts);
matches = [];

if do_plot
    colors = hsv( ntcontacts );
    hf1 = figure('Position', [680   200   900   750]);
    hold on
end

for i = 1 : ntcontacts 

    fprintf('\nContact track %d of %d.\n', i, ntcontacts )
    
    track_2 = tracks_contacts{ i };
    frames_2 = track_2(:, 3) + 1;
    pos_2 = track_2(:, 1 : 2);
    
    dpos2 = NaN(n_frames, 2);
    dpos2( frames_2, : ) = pos_2;
    
    dist_mean = NaN(ntcalcium, 1);
    dist_std = NaN(ntcalcium, 1);
    dist_n = NaN(ntcalcium, 1);
    
    for j = 1 : ntcalcium
        
        track_1 = tracks_calcium{ j };
        frames_1 = track_1(:, 3) + 1;
        pos_1 = track_1(:, 1 : 2);
        
        dpos1 = NaN(n_frames, 2);
        dpos1( frames_1, : ) = pos_1;
        
        delta = dpos2 - dpos1;
        dist = sqrt( sum(delta .* delta, 2) );
        dist_mean(j) = nanmean(dist);
        dist_std(j) = nanstd(dist);
        dist_n(j) = numel( dist( ~isnan(dist) ) );
        
    end
    
    [ val, target_id ] = min( dist_mean );
    fprintf('Matched contact track #%d with calcium track #%d,\nwith a distance of %.1f ? %.1f %s calculated over %d spots.\n', ...
        i, target_id, val, dist_std(target_id), cal.x.units, dist_n(target_id) )
    
    
    if val > max_track_dist
        fprintf('Mean distance value is above max tolerance distance. Match rejected.\n')
        continue
    end
    
    matches = [
        matches ;
        i, target_id]; %#ok<AGROW>
    
    if do_plot
        plot( pos_2(:,1), pos_2(:,2), ...
            'DisplayName', [ 'Contact #' num2str(i) ], ...
            'Color', colors(i, :), ...
            'Marker', 's', ...
            'MarkerFaceColor',  colors(i, :))
        
        track_1 = tracks_calcium{ target_id };
        pos_1 = track_1(:, 1 : 2);
        
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

        
        plot( pos_1(:,1), pos_1(:,2), ...
            'DisplayName', [ 'Calcium #' num2str(target_id) ], ...
            'Color', colors(i, :), ...
            'Marker', 'o', ...
            'MarkerFaceColor',  'w')
        
        
    end

end


if do_plot
    xlabel([ 'X (' cal.x.units ')' ] )
    ylabel([ 'Y (' cal.y.units ')' ] )
    set(gca, 'TickDir', 'out', ...
        'YDir', 'reverse', ...
        'XAxisLocation', 'top')
    axis equal
end

%% Plot kymographs.

n_matches = size(matches, 1);
fprintf('\nFound %d matches out of %d contact tracks.\n', n_matches, ntcontacts)

kymograph = zeros( 2 * n_matches, n_frames );

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

for k = 1 : n_matches
   
    i = matches(k, 1);
    j = matches(k, 2);
    
    track_2 = tracks_contacts{ i };
    track_1 = tracks_calcium{ j };

    frames_quality = track_2(:,3) + 1;
    quality = track_2(:,4);

    frames_intensity = track_1(:,3) + 1;
    intensity_raw = track_1(:,4);
    
    % Normalize min/max.
    intensity = intensity_raw / median(intensity_raw);
%     intensity = ( intensity - min(intensity) ) ./ ( max(intensity) - min(intensity) );
    
    plot(frames_intensity, intensity)
    
    contact = -0.5 * (quality > 0.01) - 0.5 * (quality > 0.2);
    kymograph( 2*k-1,   frames_quality ) = contact;
    kymograph( 2*k,     frames_intensity ) = intensity;

end

cmap = [ 
    repmat( [ 0.2 0.6 0.2 ], [ 32 1 ]);
    repmat( [ 0.2 0.2 0.6 ], [ 32 1 ]);
    0 0 0;
    hot(64) ];


hf2 = figure('Position', [ 680    50   400   800 ] );
colormap(cmap)
imagesc(kymograph)
box off 

for i = 1 : n_matches
   line( [0 n_frames], 0.5 + 2*[ i i ], ...
       'Color', 'w',...
       'LineWidth', 3)
    
end

set(gca, ...
    'TickDir', 'out', ...
    'YTick', 0.5 + 1 : 2 : 2 * n_matches, ...
    'YTickLabel', 1 : n_matches)

xlabel('Time (frames)')
ylabel('Track matches')
[~, name] = fileparts(file_calcium);
title(name, ...
    'Interpreter', 'None')


%% Save figures

return

save_name_tracks = [name '-tracks.pdf' ];
save_name_matches = [name '-matches.pdf' ];

export_fig('-r600', save_name_tracks, hf1);
export_fig('-r600', '-opengl', save_name_matches, hf2);

