
%% Clean & load.

clc
clear 
close all

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


nContacts = table( treatments, superAntigen, individual, ...
    'VariableNames', { 'treatment', 'superAntigen', 'individual' }, ...
    'RowNames', sourceFiles );

nContacts.Properties.VariableDescriptions = {
    'Treatment applied to cells.'
    'Whether super antigen was added to cells'
    'Whether this is a data from a single movie'
    };

nFiles = numel( sourceFiles );


%% Loop over all files.

n               = NaN( nFiles, 1 );
nCorrelated     = NaN( nFiles, 1 );
nAntiCorrelated = NaN( nFiles, 1 );
nNotCorrelated  = NaN( nFiles, 1 );
nLong               = NaN( nFiles, 1 );
nLongCorrelated     = NaN( nFiles, 1 );
nLongAntiCorrelated = NaN( nFiles, 1 );
nLongNotCorrelated  = NaN( nFiles, 1 );
nShort               = NaN( nFiles, 1 );
nShortCorrelated     = NaN( nFiles, 1 );
nShortAntiCorrelated = NaN( nFiles, 1 );
nShortNotCorrelated  = NaN( nFiles, 1 );

for i = 1 : nFiles
    
    sourceFile = fullfile( rootFolder, sourceFiles{ i } );
    load( sourceFile )
    
    fprintf('- %s\n', sourceFiles{ i } )
    
    nCells = numel( tcells );
    
    longContactThreshold = 60; % x10s = 10 min.
    dt = 10.; % s, frame interval.
    
    normalizationMethod = 'none';
    
    maxLag = 10;
    
    mvAvg = 21;
    
    doPlotIndividuals = false;
    doPlotCDF = false;
    
    %% Analyze long and short contacts.
    
    [ corrArray, signLimit, contactDurations, contactClipped ] = detectCorrelation(tcells, doPlotIndividuals, doPlotCDF);
    
    % Long and short contacts. Careful not to include short contacts that would
    % be clipped by the end of the movie.
    longContacts = contactDurations > longContactThreshold;
    shortContacts = (contactDurations <= longContactThreshold) & ~contactClipped;
    
    % Calcium.
    corrContacts = corrArray( : , 1 );
    antiCorrContacts = corrArray( : , 2 );
    noCorrContacts = corrArray( : , 3 );
    
    % Output.
    n( i )                  = size( corrArray, 1);
    nCorrelated( i )        = sum( corrContacts );
    nAntiCorrelated( i )    = sum( antiCorrContacts );
    nNotCorrelated( i )     = sum( noCorrContacts );
    nLong( i )                  = sum( longContacts );
    nLongCorrelated( i )        = sum( corrContacts & longContacts );
    nLongAntiCorrelated( i )    = sum( antiCorrContacts & longContacts );
    nLongNotCorrelated( i )     = sum( noCorrContacts & longContacts );
    nShort( i )                  = sum( shortContacts );
    nShortCorrelated( i )        = sum( corrContacts & shortContacts );
    nShortAntiCorrelated( i )    = sum( antiCorrContacts & shortContacts );
    nShortNotCorrelated( i )     = sum( noCorrContacts & shortContacts );
    
end

%% Flesh out and save table.


nContacts.ratioCorr             = nCorrelated ./ n ;
nContacts.ratioAntiCorr         = nAntiCorrelated ./ n ;
nContacts.ratioNotCorr          = nNotCorrelated ./ n ;

nContacts.ratioLongCorr             = nLongCorrelated ./ nLong ;
nContacts.ratioLongAntiCorr         = nLongAntiCorrelated ./ nLong ;
nContacts.ratioLongNotCorr          = nLongNotCorrelated ./ nLong ;

nContacts.ratioShortCorr             = nShortCorrelated ./ nShort ;
nContacts.ratioShortAntiCorr         = nShortAntiCorrelated ./ nShort ;
nContacts.ratioShortNotCorr          = nShortNotCorrelated ./ nShort ;

nContacts.n                     = n;
nContacts.nCorrelated           = nCorrelated;
nContacts.nAntiCorrelated       = nAntiCorrelated;
nContacts.nNotCorrelated        = nNotCorrelated;

nContacts.nLong                 = nLong;
nContacts.nLongCorrelated       = nLongCorrelated;
nContacts.nLongAntiCorrelated   = nLongAntiCorrelated;
nContacts.nLongNotCorrelated    = nLongNotCorrelated;

nContacts.nShort                = nShort;
nContacts.nShortCorrelated      = nShortCorrelated;
nContacts.nShortAntiCorrelated  = nShortAntiCorrelated;
nContacts.nShortNotCorrelated   = nShortNotCorrelated;

save( fullfile( rootFolder, 'nContacts.mat' ) , 'nContacts' )
