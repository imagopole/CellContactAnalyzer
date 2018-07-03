%% Plot correlation table.

%% Clean & load.

clc
clear 
close all

rootFolder = '/Users/tinevez/Google Drive/Projects/Contacts/MATLAB files/correlation/';
load( fullfile( rootFolder, 'nContacts.mat' ) )

%% SuperAntigen - vs +.
s = grpstats( nContacts, { 'treatment', 'superAntigen' }, { 'mean', 'std' } );

sag = s.superAntigen;


figure('Position', [ 680 1 560 1000 ] )

subplot(311)
b1 = bar( [ s.mean_ratioCorr( ~sag ), s.mean_ratioCorr( sag ) ], 1 );
set( gca, 'XTickLabel', cellstr( s.treatment( sag ) ) )
box off
ylabel( 'nc_{corr} / nc' )
title('All contacts')

for k1 = 1 : 2
    ctr(k1,:) = bsxfun(@plus, b1(1).XData, [b1(k1).XOffset]');
    ydt(k1,:) = b1(k1).YData;
end
hold on
errorbar(ctr, ydt, zeros(size(ctr)), [ s.std_ratioCorr( ~sag ) s.std_ratioCorr( sag ) ]' , '.k')
hold off
legend( { '\0', 'SAg' }, 'Box', 'off' )

%----------------------------

subplot(312)
b2 = bar( [ s.mean_ratioLongCorr( ~sag ), s.mean_ratioLongCorr( sag ) ], 1 );
set( gca, 'XTickLabel', cellstr( s.treatment( sag ) ) )
box off
ylabel( 'nc_{corr} / nc' )
title('Long contacts (> 10 min)')

for k1 = 1 : 2
    ctr(k1,:) = bsxfun(@plus, b2(1).XData, [b2(k1).XOffset]');
    ydt(k1,:) = b2(k1).YData;
end
hold on
errorbar(ctr, ydt, zeros(size(ctr)), [ s.std_ratioLongCorr( ~sag ) s.std_ratioLongCorr( sag ) ]' , '.k')
hold off

%--------------------------
subplot(313)
b3 = bar( [ s.mean_ratioShortCorr( ~sag ), s.mean_ratioShortCorr( sag ) ], 1 );
set( gca, 'XTickLabel', cellstr( s.treatment( sag ) ) )
box off
ylabel( 'nc_{corr} / nc' )
title('Short contacts (< 10 min)')

for k1 = 1 : 2
    ctr(k1,:) = bsxfun(@plus, b3(1).XData, [b3(k1).XOffset]');
    ydt(k1,:) = b3(k1).YData;
end
hold on
errorbar(ctr, ydt, zeros(size(ctr)), [ s.std_ratioShortCorr( ~sag ) s.std_ratioShortCorr( sag ) ]' , '.k')
hold off

%-----------------------------------------

b1(1).FaceColor = 'w';
b2(1).FaceColor = 'w';
b3(1).FaceColor = 'w';
b1(2).FaceColor = 'k';
b2(2).FaceColor = 'k';
b3(2).FaceColor = 'k';
