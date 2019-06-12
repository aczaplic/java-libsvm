score_method = {'score','score mmt','score delta'};
q_threshold = [0.01,0.05,0.1,0.2,0.5];
C = [0.1,1,10,50,100,500,1e3];

plot_values = zeros(15,7);
y_name = cell(15,1);
for s = 1:size(score_method,2)
    for q = 1:size(q_threshold,2)
        i = (s-1)*length(q_threshold)+q;
        %plot_values(i,:) = error_test(:,:,i);
        plot_values(i,:) = pos_idn(1,:,2,i);
        y_name{i} = [score_method{s},' q_t=',num2str(q_threshold(q))];
    end
end

h = heatmap(num2cell(C),y_name,plot_values,'Colormap',parula);
h.ColorScaling = 'log';
%h.ColorbarVisible = 'off';
xlabel('C');
