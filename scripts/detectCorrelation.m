function [ corrArray, signLimit, contactDurations, contactStarts, corrScoreOther ] = detectCorrelation(tcells, doPlotIndividuals, doPlotCDF) 

    nCells = numel( tcells );
    normalizationMethod = 'none';    
    maxLag = 10;
    
    if nargin < 2
        doPlotIndividuals = false;
    end
    if nargin < 3
        doPlotCDF = false;
    end
    
    %% Investigate correlation of calcium spikes with creation of contacts.
    
    corrScoreCells          = cell( nCells, 1 );
    contactDurationCells    = cell( nCells, 1 );
    corrOtherScoreCells     = cell( nCells, 1 );
    contactClippedCells     = cell( nCells, 1 );
    
    for i = 1 : nCells
        
        tc = tcells( i );
        x = tc.intensity;
        tx = tc.t;
        minT = min( tx );
        % Make it start at 1.
        tx = [ ( 1 : ( minT - 1 ) )' ;
            tx ]; %#ok<AGROW>
        x = [ zeros( ( minT - 1 ), 1 ) ;
            x ] ; %#ok<AGROW>
        
        y = tc.cArea;
        ty = tc.ct;
        
        maxT = min( max(tx), max(ty) );
        
        %%%%%%%%%%%%%%%%%%%%%%%
        % Calcium transients. %
        %%%%%%%%%%%%%%%%%%%%%%%
        
        % Calcium diff.
        xf = diff( x ) ./ diff( tx );
        
        %%%%%%%%%%%%%%%%%
        % Contact area. %
        %%%%%%%%%%%%%%%%%
        
        % Split in contiguous segments.
        starts = find ( diff( isnan( y ) ) == -1 ) + 1;
        if ~isnan( y(1) )
            starts = [ 1 ; starts ] ; %#ok<AGROW>
        end
        
        ends = find( diff( isnan( y ) ) == +1 );
        if ~isnan( y(end) )
            ends = [ ends ; numel(y) ] ; %#ok<AGROW>
        end
        
        
        if doPlotIndividuals
            figure
            subplot(211)
            xlabel('Frame')
            ax = plotyy(0, 0, 0, 0 );
            ha1a = ax(1);
            ha1b = ax(2);
            hold(ax(2), 'on')
            hold(ax(1), 'on')
            ylabel( ha1a, 'Calcium signal derivative' )
            ylabel( ha1b, 'Contact area derivative' )
            
            subplot(212)
            hold on
            ha2 = gca;
            xlabel('Calcium lag on contact area')
            ylabel('Cross-correlation')
            
            plot( ha1a, tx(1:end-1), xf, 'k' );
        end
        
        %%%%%%%%%%%%%%%%%%%%%%%%%
        % Investigate segments. %
        %%%%%%%%%%%%%%%%%%%%%%%%%
        
        nSegments = numel( starts );
        corrScoreSegments       = NaN( nSegments, 1 );
        contactDurationSegments = NaN( nSegments, 1 );
        contactClippedSegments  = NaN( nSegments, 1 );
        corrOtherScoreSegments  = NaN( nSegments * ( nCells - 1 ), 1 );
        
        % To index contact corr with other calcium signal.
        lindex = 0;
        
        for k = 1 : nSegments
            
            segment = starts( k ) : ends( k );
            ys = y(segment);
            tys = ty(segment);
            
            % Pad
            tysp = [ ...
                ( 1 : ( tys( 1 ) - 1 ) )' ; ...
                tys ; ...
                ( ( tys( end ) + 1 ) : maxT )'...
                ];
            
            ysp = zeros( numel(tysp), 1 );
            ysp( segment ) = ys;
            
            % Derive
            dysp = diff( ysp ) ./ diff( tysp );
            
            % Cross-correlation.
            [ r, lags ] = xcorr( xf, dysp, maxLag, normalizationMethod );
            
            % Measure the xcorr @ lag = 0.
            i0 = find( lags == 0 );
            corrScoreSegments( k )          = r( i0 ); %#ok<FNDSB>
            contactDurationSegments( k )    = numel( segment );
            contactClippedSegments( k )     = max( tys ) == maxT;
            
            % Now measure the correlation of this contact increase in size,
            % with the calcium signal of all OTHER cells.
            for l = 1 : nCells
                
                if l == i
                    continue;
                end
                lindex = lindex + 1;
                
                tcOther = tcells( l );
                xOther = tcOther.intensity;
                txOther = tcOther.t;
                minTother = min( txOther );
                % Make it start at 1.
                txOther = [ ( 1 : ( minTother - 1 ) )' ;
                    txOther ]; %#ok<AGROW>
                xOther = [ zeros( ( minTother - 1 ), 1 ) ;
                    xOther ] ; %#ok<AGROW>
                xfOther = diff( xOther ) ./ diff( txOther );
                [ rOther, lagsOther ] = xcorr( xfOther, dysp, maxLag, normalizationMethod );
                
                % Measure the xcorr @ lag = 0.
                i0 = find( lagsOther == 0 );
                corrOtherScoreSegments( lindex ) = rOther( i0 ); %#ok<FNDSB>
            end
            
            if doPlotIndividuals
                hpc = plot(ha1b, tysp( 1: end-1 ), dysp);
                plot( ha2, lags, r, '.-', ...
                    'Color', get( hpc, 'Color' ) )
            end
            
        end
        
        corrScoreCells{ i }         = corrScoreSegments;
        contactDurationCells{ i }   = contactDurationSegments;
        contactClippedCells{ i }      = contactClippedSegments;
        corrOtherScoreCells{ i }    = corrOtherScoreSegments;
        
        if doPlotIndividuals
            set(ax, 'XLimMode', 'auto' , ...
                'YLimMode', 'auto', ...
                'XTickMode', 'auto', ...
                'YTickMode', 'auto')
        end
        
    end
    
    corrScore           = vertcat( corrScoreCells{ : } );
    contactDurations    = vertcat( contactDurationCells{ : } );
    contactStarts       = vertcat( contactClippedCells{ : } );
    corrScoreOther      = vertcat( corrOtherScoreCells{ : } );
    % clear corrScoreCells corrScoreSegments corrOtherScoreCells corrOtherScoreSegments
    
    %% Determine significance limit.

    % 2 x sigma -> 95%  within
    signLimit = 2. * std( corrScoreOther );
    
    %% Plot histograms.
    
    if doPlotCDF
        figure
        hold on
        hcs = cdfplot( corrScore );
        set(hcs, 'DisplayName', 'B cell vs T cell', 'LineWidth', 2  )
        hcr = cdfplot( corrScoreOther );
        set(hcr, 'DisplayName', 'random cells', 'LineWidth', 2 )
        
        % Plot 2 sigmas limit.
        line( [ -signLimit -signLimit ], [ 0 1 ], 'Color', 'k', 'LineStyle', '--', 'DisplayName', 'Significance limit' )
        
        xlabel('Xcorr @ lag=0')
        ylabel('Probability')
        hl = legend('toggle');
        set( hl, 'box', 'off', 'Location', 'southeast' )
        xlim( [ -20000 60000 ] )
        
        line( [ signLimit signLimit ], [ 0 1 ], 'Color', 'k', 'LineStyle', '--' )        
    end
    
    %% Yield correlation.

    corrContacts = corrScore > signLimit;
    antiCorrContacts = corrScore < -signLimit;
    noCorrContacts = ~( corrContacts | antiCorrContacts );
    
    corrArray = [ corrContacts, antiCorrContacts, noCorrContacts ];
    
end
