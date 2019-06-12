filename = './data/K_0_4_F060457.txt';
fig_loc = './figures/rbf';
score_method = {'score','score_mmt','score_delta'};
q_threshold = [0.01,0.05,0.1,0.2,0.5];
use_all_decoy = 1;
C = [0.1,1,10,50,100,500,1e3];
gamma = [0.01,0.05,0.1,0.5,1,5,10];

%wczytanie pelnego zbioru (do testow)
dataset = importdata(filename);
full_data = dataset.data;
if ~isempty(full_data)
    pos_neg_F = full_data(:,1);
    full_data = full_data(:,2:end);
end

for s = 1:size(score_method,2)
    %wybor miary score Mascota
    switch score_method{s}
        case 'score'
            score=full_data(:,5);
        case 'score_mmt'
            score=full_data(:,5)-min(full_data(:,6),full_data(:,7));
        case 'score_delta'
            score=full_data(:,8);
    end
    
    %wyznaczenie q-wartosci na podstawie uporzadkowania zgodnego z wybranym score Mascota
    qM = get_fdr(pos_neg_F,score);
    plot_qM = sort(qM(pos_neg_F==0));
    
    for q = 1:size(q_threshold,2)
        sq = (s-1)*length(q_threshold)+q;

        for c = 1:size(C,2)
            
            for g = 1:size(gamma,2)
                
                %rysowanie histogramu nowego score (wyznaczonego przez siec)
                title_spec = ['score ',score_method{s}(7:end),' z q_t=',num2str(q_threshold(q)),', C=',num2str(C(c)),', gamma=',num2str(gamma(g))];
                name = [', gamma=',num2str(gamma(g))];
                title({'SVM score'; ['\fontsize{10}',title_spec]});
                
                figure(2)
                plot(plot_q_svm{g,c,sq},1:length(plot_q_svm{g,c,sq}),'DisplayName',['SVM score',name]);
                hold on
            end
            
        figure(2);
        plot(plot_qM,1:length(plot_qM),'-k','LineWidth',1.2,'DisplayName','Mascot score');
        title({'Zależność liczby prawidłowych identyfikacji';'z bazy target od q-wartości'; ['\fontsize{10}',title_spec]});
        xlabel('q-wartości');ylabel('liczba prawidłowo zidentyfikowanych peptydów');
        set(gca,'XLim',[0 0.2]);
        legend('Location','SouthEast')
        hold off
        fig_spec = [score_method{s},'_q',num2str(q_threshold(q)),'_C',num2str(C(c)),'.png'];
        saveas(gcf,[fig_loc,'_qvalues/q_',fig_spec])
        
        end
    end
end

