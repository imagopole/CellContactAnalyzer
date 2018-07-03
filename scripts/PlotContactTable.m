%% Plot statistics on contacts.


%% Clean & load.

clc
clear 
close all

rootFolder = '/Users/tinevez/Google Drive/Projects/Contacts/MATLAB files/correlation/';
load( fullfile( rootFolder, 'contactsTable.mat' ) )

%% Grouping and pruning.

[ G, labels ] = internal.stats.mgrp2idx( ...
    { contactsTable.treatment, contactsTable.superAntigen }, ...
    [], '_' );

s = contactsTable;


donotplot = G <= 2 | G >= 7;
s( donotplot, :) = [];
G( donotplot ) = [];
labels = labels( 3 : 6 );

x = [ 0.8, 1.2, 1.8, 2.2 ];
G( G == 3 ) = 0.8;
G( G == 4 ) = 1.2;
G( G == 5 ) = 1.8;
G( G == 6 ) = 2.2;

figure('Position', [ 680 1 560 1000 ] )


%% N short contacts.

subplot(311)
hb1 = notBoxPlot( s.nShortContacts, G, 'jitter', 0.3 );
adjustNotBoxPlot( hb1 )
ylim( [ 0 10 ] )
title('Short contacts for T-cells with at least 1 contact.')
ylabel( 'Number of short contacts per T-cell' )

genSigStar( s.nShortContacts, G );
legend( [hb1(1).sdPtch, hb1(2).sdPtch ] , { '\0', 'SAg' }, 'Box', 'off', 'Location', 'northwest' ) 

%% Short contact duration.

subplot(312)
hb2 = notBoxPlot( s.shortContactDurationMean, G, 'jitter', 0.3 );
adjustNotBoxPlot( hb2 )
ylabel( sprintf('Short contact duration (%s)', contactsTable.Properties.VariableUnits{6} ) )
genSigStar( s.shortContactDurationMean, G );

%% Short contact area.

subplot(313)
hb3 = notBoxPlot( s.shortContactMaxAreaMean, G, 'jitter', 0.3 );
adjustNotBoxPlot( hb3 )
ylabel( sprintf('Short contact max area (%s)', contactsTable.Properties.VariableUnits{8} ) )
genSigStar( s.shortContactMaxAreaMean, G );

%-----------------------------------------

function adjustNotBoxPlot( b1 )
    box off
    xlim( [ 0.5 2.5 ] )
    set( [b1([1,3]).sdPtch], 'FaceColor', [ 0.6 0.6 1 ], 'EdgeColor', [0.48 0.48 0.8] )
    set( [b1([1,3]).semPtch], 'FaceColor', [ 0.3 0.3 1 ], 'EdgeColor', [0.48 0.48 0.8] )
    set( [b1([2,4]).sdPtch], 'FaceColor', [ 1 0.6 0.6 ], 'EdgeColor', [ 0.8 0.48 0.48] )
    set( [b1([2,4]).semPtch], 'FaceColor', [ 1 0.3 0.3 ], 'EdgeColor', [ 0.8 0.48 0.48] )
    set( [b1.mu], 'Color', 'k' )
    set( gca, 'XTick', [ 1 2 ], ...
        'XTickLabel', { 'SiC', 'SiT' } )
    yl = ylim;
    yl ( yl < 0 ) = 0;
    ylim( yl )
end


function genSigStar( x, G )

    ug = unique( G );
    nug = numel( ug );
    couples = {};
    pvals = [];
    for i = 1 : nug - 1
        for j = i + 1 : nug
            
            x1 = x( G == ug( i ) );
            x2 = x( G == ug( j ) );
            [h, p] = ttest2( x1 , x2 );
            if h 
                couples = [
                    couples
                    [ ug( i ) , ug( j ) ] 
                    ]; %#ok<AGROW>
                pvals = [
                    pvals 
                    p ]; %#ok<AGROW>
            end            
        end
    end
    nc = numel( couples );
    for i = 1 : nc
        sigstar( couples{i}, pvals(i) )
    end

end
