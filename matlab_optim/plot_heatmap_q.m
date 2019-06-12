score_method = {'score','score mmt','score delta'};
%plot_values = pos_idn(:,:,2,:);
plot_values = error_test;
minimum = min(plot_values,[],'all');
maximum = max(plot_values,[],'all');
range = maximum-minimum;

for s = 1:size(score_method,2)
    for q = 1:size(q_threshold,2)
        i = (s-1)*length(q_threshold)+q;
        ax = subplot(3,5,i);
        %h = heatmap(num2cell(gamma),num2cell(C),pos_idn(:,:,2,i),'Colormap',parula);
        imagesc(gamma,C,plot_values(:,:,i))
        colormap(ax,spring);
        %h = heatmap(num2cell(gamma),num2cell(C),plot_values(:,:,i),'Colormap',spring);
        %caxis([minimum+0.1*range maximum-0.1*range])
        set(gca,'ColorScale','log')
%         xticks(gamma)
%         yticks(C)
%         xtickangle(45)
        set(gca, 'XTick', gamma, 'XTickLabel', num2cell(gamma))
        set(gca, 'YTick', C, 'YTickLabel', num2cell(C))
        %c.ColorScaling = 'log';
        %c.ColorbarVisible = 'off';
        title(['\fontsize{8}',score_method{s},' q_t=',num2str(q_threshold(q))]);
        %c.FontSize = 8;
    end
end