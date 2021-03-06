%% Matches two TrackMate tracks based on their spot-spot distance.
% The tracks of the two files are loaded as list of spots. 

close all
clear
clc

% Calcium TrackMate XML file.
fileCalcium = '/Users/tinevez/Google Drive/Projects/Contacts/raw data/2016-01-21/Trackmate files/SiT+SAg_1_20_Calcium.xml';

% Contact TrackMate XML file.
fileContacts = '/Users/tinevez/Google Drive/Projects/Contacts/raw data/2016-01-21/Trackmate files/SiT+SAg_1_20_Contacts.xml';

% Minimal number of edges in calcium tracks below which tracks are not
% considered.
minNEdges = 60;

% Minimal number of edges in contact tracks below which tracks are not
% considered.
minNContactEdges = 5;

% Maximal mean distance between two tracks to accept a match.
maxTrackDist = 20; % physical units

% Plot tracks?
doPlot = true;

% Save results structure? If true, data will be saved in the same folder
% that of the calcium file, with the same name, but with .mat as extension.
doSave = true;

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
    { 'POSITION_X', 'POSITION_Y', 'FRAME', 'QUALITY', 'RADIUS' } );

fprintf('Done.\n')

%% Filter contact tracks.

nEdgesTracksContacts = cellfun(@(x) size(x, 1), tracksContacts );
tracksContacts( nEdgesTracksContacts < minNContactEdges ) = [];
tracksContactsNames( nEdgesTracksContacts < minNContactEdges ) = [];

fprintf('Retaining %d tracks out of %d.\n', numel( tracksContacts ), numel( nEdgesTracksContacts ) )

%% Match.
fprintf('Matching calcium tracks to possible several contact tracks.\n')

ntCalcium = numel(tracksCalcium);
ntContacts = numel(tracksContacts);
matches = [];

if doPlot
    colors = 0.8 * hsv( ntCalcium );
    hf1 = figure('Position', [680   200   900   750]);
    hold on
end

tcells = [];
for i = 1 : ntCalcium 

    fprintf('\nCalcium track %d of %d.\n', i, ntCalcium )
    
    trackCalcium = tracksCalcium{ i };
    framesCalcium = trackCalcium.FRAME + 1;
    posCalcium = [ trackCalcium.POSITION_X trackCalcium.POSITION_Y ];
    
    dpos2 = NaN( nFrames, 2 );
    dpos2( framesCalcium, : ) = posCalcium;
    
    distMean    = NaN( ntContacts, 1);
    distStd     = NaN( ntContacts, 1);
    distN       = NaN( ntContacts, 1);
    
    for j = 1 : ntContacts
        
        trackContact = tracksContacts{ j };
        framesContact = trackContact.FRAME + 1;        
        posContact = [ trackContact.POSITION_X trackContact.POSITION_Y ];
        
        dpos1 = NaN( nFrames, 2 );
        dpos1( framesContact, : ) = posContact;
        
        delta = dpos1 - dpos2;
        dist = sqrt( sum(delta .* delta, 2) );
        distMean(j) = nanmean(dist);
        distStd(j) = nanstd(dist);
        distN(j) = numel( dist( ~isnan(dist) ) );
        
    end
    
    closeContacts = find( distMean < maxTrackDist );
    nCloseContacts = numel( closeContacts );
    
    
    if nCloseContacts == 0
        fprintf('Did not find a contact close to this calcium track.\n')
        continue
    end

    fprintf('Found %d contact tracks that matches calcium track #%d:\n', ...
        nCloseContacts, i )
    
    % Possible split contact track in several segments.
    
    contacts = {};
    for j = 1 : nCloseContacts
        
        targetId = closeContacts(j);
        fprintf('\tcontact #%d -> calcium #%d - dist %.1f +/- %.1f %s N = %d spots.\n', ...
            targetId, i, distMean(targetId), distStd(targetId), cal.x.units, distN(targetId) )
        
        trackContact = tracksContacts{ targetId };
        posContact = [ trackContact.POSITION_X trackContact.POSITION_Y ];
        framesContact = trackContact.FRAME + 1;
        radiusContact = trackContact.RADIUS;
        segmentBreaks = find ( diff(framesContact) > 1 );
        nSegments = numel( segmentBreaks ) + 1;
        fprintf( '\tfound %d segments in the contact track.\n', nSegments )
        
        segmentBreaks = [ 0 ; segmentBreaks ; numel( framesContact ) ]; %#ok<AGROW>
        for k = 1 : nSegments
            segmentStart = segmentBreaks( k ) + 1;
            segmentStop = segmentBreaks( k + 1 );
            t = framesContact( segmentStart : segmentStop );
            xy = posContact( segmentStart : segmentStop, : );
            r = radiusContact( segmentStart : segmentStop );
            contact = [ t xy r ];
            contacts = [
                contacts
                contact
                ]; %#ok<AGROW>
        end
    end
    
    nContacts = numel( contacts );
    
    % Check whether we have several simultaneous contacts for a single
    % T-cell. In that case we reject it.
    overlappingContacts = false;
    for j = 1 : nContacts - 1
        for k = j + 1 : nContacts
            
            t1 = contacts{j}(:,1);
            t2 = contacts{k}(:,1);
            if ~isempty( intersect( t1, t2 ) )
                overlappingContacts = true;
                break;
            end
            
        end
    end
    if overlappingContacts
         fprintf( '\tfound overlapping contacts for this cell. Skipping.\n')
        continue
    end
    
    % Prepare output structure.
    
    tcell = struct();
    tcell.trackID = i;
    tcell.trackName = tracksCalciumNames{ i };
    tcell.pos = posCalcium;
    tcell.t = framesCalcium;
    tcell.intensity = trackCalcium.MEAN_INTENSITY;
    tcell.nContacts = nContacts;
    
    nt = numel( tcell.t );
    cPos = NaN( nt, 2 );
    ct  = NaN( nt, 1);
    cArea = NaN( nt, 1 );
    
    for j = 1 : nContacts
        contact = contacts{ j };
        t1 = contact( : , 1 );
        pos1 = contact( : , 2 : 3  );
        r1 = contact( : , 4 );
        
        [ ~, it, it1 ] = intersect( tcell.t, t1 );
        ct( it ) = t1( it1 );
        cPos( it, : ) = pos1( it1, : );
        cArea( it ) = 2. * pi  .* r1( it1 ) .* r1( it1 );  
    end
    
    tcell.ct = ct;
    tcell.cArea = cArea;
    tcell.cPos = cPos;
    
    tcells =  [
        tcells
        tcell
        ]; %#ok<AGROW>
    
    if doPlot
        
        plot( posCalcium(:,1), posCalcium(:,2), ...
            'DisplayName', [ 'Calcium #' num2str(i) ], ...
            'Color', colors(i, :), ...
            'Marker', 's', ...
            'LineWidth', 2, ...
            'MarkerFaceColor',  colors(i, :))
        
        lx = min( posCalcium(:,1) );
        ux = max( posCalcium(:,1) );
        ly = min( posCalcium(:,2) );
        uy = max( posCalcium(:,2) );
        
        for j = 1 : nContacts
            
            contact = contacts{ j };
            plot( contact(:,2), contact(:,3), ...
                'DisplayName', [ 'Contact #' num2str(j) ], ...
                'Color', colors(i, :), ...
                'Marker', 'o', ...
                'MarkerFaceColor',  'w')
            
            lx = min(  [ lx ; contact(:, 2) ] );
            ux = max(  [ ux ; contact(:, 2) ] );
            ly = min(  [ ly ; contact(:, 3) ] );
            uy = max(  [ uy ; contact(:, 3) ] );
            
        end
        
        rectangle('Position', [ lx ly (ux-lx) (uy-ly) ], ...
            'EdgeColor', colors(i, :))
                
        text( ux , uy, ...
            [' Calcium: '  tracksCalciumNames{ i } ], ...        
            'HorizontalAlignment', 'left', ...
            'VerticalAlignment', 'bottom', ...
            'Color', colors(i, :), ...
            'Interpreter', 'None')

    end

end


if doPlot
    xlabel([ 'X (' cal.x.units ')' ] )
    ylabel([ 'Y (' cal.y.units ')' ] )
    set(gca, 'TickDir', 'out', ...
        'YDir', 'reverse', ...
        'XAxisLocation', 'top')
    axis equal
    
    [ pathstr, name, ~ ] = fileparts( fileCalcium );
    figFilePath = fullfile( pathstr, [ name '.pdf' ] );
    export_fig( '-r600', figFilePath )
    fprintf( '\nFigure saved as %s\n', figFilePath )
end

%% Save.

if doSave
    
    [ pathstr, name, ext ] = fileparts( fileCalcium );
    targetMatFile = fullfile( pathstr, [ name '.mat' ] );
    save(targetMatFile, 'tcells')
    fprintf( '\nData saved as %s\n', targetMatFile )
    
end