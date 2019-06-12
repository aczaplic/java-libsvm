score_method = {'score','score mmt','score delta'};
q_threshold = [0.01,0.05,0.1,0.2,0.5];
C = [0.1,1,10,50,100,500,1e3];
gamma = [0.01,0.05,0.1,0.5,1,5,10];

plot_values = pos_idn(:,:,2,:);
%plot_values = error_test;
minimum = min(plot_values,[],'all');
maximum = max(plot_values,[],'all');
range = maximum-minimum;

for s = 1:size(score_method,2)
    for q = 1:size(q_threshold,2)
        i = (s-1)*length(q_threshold)+q;
        ax = subplot(3,5,i);
        
        imagesc(C,gamma,plot_values(:,:,i))
        colormap(ax,parula)
        %caxis([minimum+0.1*range maximum-0.1*range])
        set(gca,'ColorScale','log')
        set(gca, 'XTick', linspace(0.1,1e3,7), 'XTickLabel', C)
        set(gca, 'YTick', linspace(0.01,10,7), 'YTickLabel', gamma)
        ax.FontSize = 7;
        title(['\fontsize{8}',score_method{s},' q_t=',num2str(q_threshold(q))]);
        
%         h = heatmap(num2cell(C),num2cell(gamma),plot_values(:,:,i),'Colormap',parula);
%         h.ColorScaling = 'log';
%         h.ColorbarVisible = 'off';
%         h.Title = [score_method{s},' q_t=',num2str(q_threshold(q))];
%         h.FontSize = 8;
        
        xlabel('C');
        ylabel('gamma');
    end
end