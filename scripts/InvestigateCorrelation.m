
%% Clean & load.

clc
clear 
close all

treatment = categorical( { 'NE' } );
superAntigen = true;

sourceFile = '/Users/tinevez/Google Drive/Projects/Contacts/MATLAB files/correlation/D4.mat';
load( sourceFile )

nCells = numel( tcells );

longContactThreshold = 60; % x10s = 10 min.
dt = 10.; % s, frame interval.

normalizationMethod = 'none';

maxLag = 10;

mvAvg = 21;

doPlotIndividuals = false;
doPlotCDF = true;

%% Analyze long and short contacts.

[ corrArray, signLimit, contactDurations, contactClipped, corrScoreOther ] = detectCorrelation(tcells, doPlotIndividuals, doPlotCDF); 

% Long and short contacts. Careful not to include short contacts that would
% be clipped by the end of the movie.
longContacts = contactDurations > longContactThreshold;
shortContacts = (contactDurations <= longContactThreshold) & ~contactClipped;

% Calcium.
corrContacts = corrArray( : , 1 );
antiCorrContacts = corrArray( : , 2 );
noCorrContacts = corrArray( : , 3 );

% Output.
clc
fprintf('For file %s.\n\n', sourceFile )

fprintf('Amongst ALL contacts formation (N = %d):\n', size( corrArray, 1) )
fprintf('\tCorrelated with calcium burst:\t\t%5.1f %%\n', 100. * sum( corrContacts) / numel( corrContacts ) )
fprintf('\tAnti-correlated with calcium burst:\t%5.1f %%\n', 100. * sum( antiCorrContacts) / numel( antiCorrContacts ) )
fprintf('\tNot correlated with calcium burst:\t%5.1f %%\n', 100. * sum( noCorrContacts) / numel( noCorrContacts ) )

fprintf('\n')
fprintf('Amongst LONG contacts formation (N = %d, dT > %.1f min):\n', sum( longContacts ), longContactThreshold * dt / 60. )
fprintf('\tCorrelated with calcium burst:\t\t%5.1f %%\n', 100. * sum( corrContacts & longContacts ) / sum( longContacts ) )
fprintf('\tAnti-correlated with calcium burst:\t%5.1f %%\n', 100. * sum( antiCorrContacts& longContacts ) / sum( longContacts ) )
fprintf('\tNot correlated with calcium burst:\t%5.1f %%\n', 100. * sum( noCorrContacts& longContacts ) / sum( longContacts ) )

fprintf('\n')
fprintf('Amongst SHORT contacts formation (N = %d, dT <= %.1f min):\n', sum( shortContacts ), longContactThreshold * dt / 60. )
fprintf('\tCorrelated with calcium burst:\t\t%5.1f %%\n', 100. * sum( corrContacts & shortContacts ) / sum( shortContacts ) )
fprintf('\tAnti-correlated with calcium burst:\t%5.1f %%\n', 100. * sum( antiCorrContacts& shortContacts ) / sum( shortContacts ) )
fprintf('\tNot correlated with calcium burst:\t%5.1f %%\n', 100. * sum( noCorrContacts& shortContacts ) / sum( shortContacts ) )

fprintf('\n')
fprintf('Negative controls wrongly classified as correlated by detection:\t%5.1f %%\n', 100. * sum(corrScoreOther > 2. * signLimit) / numel( corrScoreOther ) )

