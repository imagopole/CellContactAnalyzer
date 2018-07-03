%% Harvest various statistics on contacts and calcium pulses.


%% Clean & load.

clc
clear 
close all

dt = 10. / 60.; % min, frame interval.
longContactThreshold = 60; % x10s = 10 min.

rootFolder = '/Users/tinevez/Google Drive/Projects/Contacts/MATLAB files/correlation/';
sourceFiles = {
    % SiC - SAg
    'A1.mat'
    'A2.mat'
    'A3.mat'
    'A4.mat'
    % SiC + SAg
    'B1.mat'
    'B2.mat'
    'B4.mat'
    'B5.mat'
    % NE - SAg
    'C1.mat'
    'C2.mat'
    % NE + SAg
    'D1.mat'
    'D2.mat'
    'D3.mat'
    'D4.mat'
    % SiT - SAg
    'E1.mat'
    'E2.mat'
    'E3.mat'
    % SiT + SAg
    'F1.mat'
    'F2.mat'
    'F3.mat'
    'F4.mat'
    % All SiC - SAg
    'allA.mat'
    % All SiC + SAg
    'allB.mat'
    % All NE - SAg
    'allC.mat'
    % All NE + SAg
    'allD.mat'
    % All SiT - SAg
    'allE.mat'
    % All SiT + SAg
    'allF.mat'
    };
treatments = categorical( {
    %
    'SiC'
    'SiC'
    'SiC'
    'SiC'
    %
    'SiC'
    'SiC'
    'SiC'
    'SiC'
    %
    'NE'
    'NE'
    %
    'NE'
    'NE'
    'NE'
    'NE'
    % 
    'SiT'
    'SiT'
    'SiT'
    %
    'SiT'
    'SiT'
    'SiT'
    'SiT'
    %
    'all SiC'
    'all SiC'
    'all NE'
    'all NE'
    'all SiT'
    'all SiT'
} );
superAntigen = [
    false
    false
    false
    false
    %
    true 
    true
    true
    true
    %
    false
    false
    %
    true
    true
    true
    true
    %
    false
    false
    false
    %
    true
    true
    true
    true
    %
    false
    true
    false
    true
    false
    true
];
individual = true( numel( sourceFiles ), 1 );
individual( end-5 : end ) = false;

%% Loop over files and build data table.

nFiles = numel( sourceFiles );
contactsTable = table();
for i = 1 : nFiles
   
    data = load( fullfile( rootFolder, sourceFiles{i} ) );
    nTCells = numel( data.tcells );
    for j = 1 : nTCells
       
        tc = data.tcells( j );
        
        tcRow = struct();
        tcRow.superAntigen = logical( superAntigen( i ) );
        tcRow.individual = individual( i );
        tcRow.treatment = treatments( i );
        
        % Split in contiguous segments.
        starts = find ( diff( isnan( tc.cArea ) ) == -1 ) + 1;
        if ~isnan( tc.cArea(1) )
            starts = [ 1 ; starts ] ; %#ok<AGROW>
        end
        
        ends = find( diff( isnan( tc.cArea ) ) == +1 );
        if ~isnan( tc.cArea(end) )
            ends = [ ends ; numel( tc.cArea ) ] ; %#ok<AGROW>
        end
        
        % Number of contacts.
        nContacts = numel( starts );
        
        % Contact areas.
        maxAreas = [];
        for k = 1 : nContacts
           
            ca = tc.cArea( starts(k) : ends(k) );
            maxAreas = [
                maxAreas
                max( ca ) ]; %#ok<AGROW>
        end
        maxAreaMean = mean( maxAreas );
        maxAreaStd = std( maxAreas );
        
        % Contact durations. We prune the contacts that do not end before
        % the cell leaves the field or the movie ends. Or contacts that
        % started before the cell was imaged. Or contacts that are too
        % long.
        toRemove = [];
        for k = 1 : nContacts
            if starts( k ) == tc.t( 1 ) ...
                    ||  ends( k ) == tc.t( end ) ...
                    || ( ends(k)-starts(k) ) > longContactThreshold
                toRemove = [ toRemove ; k ]; %#ok<AGROW>
            end
        end
        starts( toRemove ) = [];
        ends( toRemove ) = [];
        maxAreas( toRemove ) = []; %#ok<SAGROW>

        contactDuration = dt * ( ends - starts );
        
        tcRow.nContacts = nContacts;
        tcRow.nShortContacts = numel( ends );
        tcRow.shortContactDurationMean = mean( ends - starts ) * dt;
        tcRow.shortContactDurationStd = std( ends - starts ) * dt;
        
        tcRow.maxAreaMean = maxAreaMean;
        tcRow.maxAreaStd = maxAreaStd;
        tcRow.shortContactMaxAreaMean = mean( maxAreas );
        tcRow.shortContactMaxAreaStd = std( maxAreas );
        
        contactsTable = [
            contactsTable 
            struct2table( tcRow ) ]; %#ok<AGROW>
        
    end
    
end

contactsTable.superAntigen = logical( contactsTable.superAntigen );
contactsTable.Properties.VariableUnits = { '', '', '', ...
    '', '',  ...
    'min', 'min', ...
    'um^2', 'um^2', ...
    'um^2', 'um^2' };

save( fullfile( rootFolder, 'contactsTable.mat' ) , 'contactsTable' )
