function [ tracks, trackNames ] = loadtracks( filePath, spotFeatures )
%LOADTRACKS Load a TrackMate file and builds linear tracks.

    %% Load data.
    
    fprintf('Loading spots... ')
    [ spotTable, spotIDMap ] = trackmateSpots(filePath, spotFeatures);
    fprintf('Done.\n')
    fprintf('Loading edges... ')
    trackMap = trackmateEdges( filePath, { 'SPOT_SOURCE_ID', 'SPOT_TARGET_ID' } );    
    fprintf('Done.\n')
    
    %% Build tracks.

    trackNames = trackMap.keys;
    nTracks = numel( trackNames );
    tracks = cell( nTracks, 1 );
    for i = 1 : nTracks
       
        trackName = trackNames{ i };
        trackTable = trackMap( trackName );
        ids = [ trackTable.SPOT_SOURCE_ID ; trackTable.SPOT_TARGET_ID  ];
        uniqueIDs = unique( ids );
        nSpots = numel( uniqueIDs );
        
        spotRows = NaN( nSpots, 1 );
        for j = 1 : nSpots
            spotRows( j ) = spotIDMap( uniqueIDs( j ) );
        end
        
        tracks{ i } = sortrows( spotTable( spotRows, : ), 'FRAME' );
    end
end
