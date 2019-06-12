function [SVMmodels, SVMscore, plot_q_svm, nr_sv, error_test, error_train, pos_idnM, pos_idn] = test_libsvm_rbf()
kernel_type = 'rbf';
kernel = 2;
filename = './data/K_0_4_F060457.txt';
fig_loc = ['./figures/',kernel_type];
    
score_method = {'score','score_mmt','score_delta'};
q_threshold = [0.01,0.05,0.1,0.2,0.5];
use_all_decoy = 1;
C = 1;
gamma = [0.01,0.05,0.1,0.5,1,5,10,50,100,1000];
%gamma = 10;
%penalty = [0,1;1,0];%sum(D_train)/(size(D_train,1)-sum(D_train));1,0];

%wczytanie pelnego zbioru (do testow)
dataset = importdata(filename);
full_data = dataset.data;
if ~isempty(full_data)
    pos_neg_F = full_data(:,1);
    full_data = full_data(:,2:end);
end

k = size(score_method,2)*size(q_threshold,2);
pos_idnM = zeros(size(score_method,2),3);
pos_idn = zeros(size(gamma,2),3,k);
SVMmodels = cell(size(gamma,2),k);
SVMscore = cell(size(gamma,2),k);
plot_q_svm = cell(size(gamma,2),k);
nr_sv = zeros(size(gamma,2),k);
error_test = zeros(size(gamma,2),k);
error_train = zeros(size(gamma,2),k);

for s = 1:size(score_method,2)
    fprintf([score_method{s},'\n']);
    
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
    pos_idnM(s,1) = length(find(plot_qM<=0.01));
    pos_idnM(s,2) = length(find(plot_qM<=0.05));
    pos_idnM(s,3) = length(find(plot_qM<=0.1));
    
    for q = 1:size(q_threshold,2)
        fprintf('q = %d\n', q_threshold(q));
        
        sq = (s-1)*length(q_threshold)+q;
        
        %stworzenie zbioru uczacego
        %new_filename = learning_set(filename,q_threshold(q),use_all_decoy,score_method{s});
        new_filename = [filename(1:end-4),'_',num2str(q_threshold(q)),'_',num2str(use_all_decoy),'_',score_method{s},'.txt'];
        
        %odczyt pliku
        new_dataset = importdata(new_filename);
        data = new_dataset.data;
        
        if ~isempty(data)
            %pobranie danych i informacji o przynaleznosci do klas
            pos_neg = data(:,1);
            data = data(:,2:end);
            
            %normalizacja danych
            minimum = min(data,[],1);
            maximum = max(data,[],1);
            data = data-repmat(minimum,size(data,1),1);
            data = data./repmat((maximum-minimum),size(data,1),1);
            
            full_data_norm = full_data-repmat(minimum,size(full_data,1),1);
            full_data_norm = full_data_norm./repmat((maximum-minimum),size(full_data_norm,1),1);
        end
        
        for g = 1:size(gamma,2)
            fprintf('gamma = %d\n', gamma(g));
            
            %tworzenie i sprawdzenie modelu SVM
            options = ['-s 0 -t ',num2str(kernel),' -c ',num2str(C),' -g ',num2str(gamma(g))];
            SVMmodels{g,sq} = svmtrain(pos_neg,sparse(data),options);
            sv = SVMmodels{g,sq}.sv_indices;
            nr_sv(g,sq) = size(sv,1);
            
            label = svmpredict(pos_neg, sparse(data), SVMmodels{g,sq});
            error = label==pos_neg;
            error_train(g,sq) = (sum(~error))/size(pos_neg,1);
            
            %sprawdzenie dzialania modelu na calym zbiorze danych
            [label, ~, SVMscore{g,sq}] = svmpredict(pos_neg_F, sparse(full_data_norm), SVMmodels{g,sq});
            error = label==pos_neg_F;
            error_test(g,sq) = (sum(~error)-length(find(pos_neg_F==2)))/(size(pos_neg_F,1)-length(find(pos_neg_F==2)));
            if (pos_neg(1)==1)
                SVMscore{g,sq} = -SVMscore{g,sq};
            end
            
            %rysowanie histogramu nowego score (wyznaczonego przez siec)
            hist_plot(~pos_neg_F,SVMscore{g,sq},50,'SVM score');
            fig_spec = [score_method{s},'_q',num2str(q_threshold(q)),'_g',num2str(gamma(g)),'.png'];
            title_spec = ['score ',score_method{s}(7:end),' z q_t=',num2str(q_threshold(q))];
            name = [', gamma=',num2str(gamma(g))];
            title({'SVM score'; ['\fontsize{10}',title_spec,name]});
            saveas(gcf,[fig_loc,'_SVMscore/hist_',fig_spec])
            
            %posortowanie danych zgodnie z nowym score
            [~,ind] = sort(SVMscore{g,sq},'descend');
            pos_neg_sort = pos_neg_F(ind);
            %wyznaczenie q-wartosci na podstawie uporzadkownia zgodnego z mscore
            qSVM = get_fdr(pos_neg_sort);
            
            %do wyrysowania zaleznosci liczby identyfikacji z bazy target od q-wartosci (w pełnym zakresie i [0,0.1])
            plot_q_svm{g,sq} = sort(qSVM(pos_neg_sort==0));
            pos_idn(g,1,sq) = length(find(plot_q_svm{g,sq}<=0.01));
            pos_idn(g,2,sq) = length(find(plot_q_svm{g,sq}<=0.05));
            pos_idn(g,3,sq) = length(find(plot_q_svm{g,sq}<=0.1));
            
            %{
                figure;
                plot(plot_qM,1:length(plot_qM),plot_q_svm{g,c,sq},1:length(plot_q_svm{g,c,sq}));
                title({'Zależność liczby prawidłowych identyfikacji';'z bazy target od q-wartości'});
                xlabel('q-wartości');ylabel('liczba prawidłowo zidentyfikowanych peptydów');
                legend('Mascot score','SVM score','Location','SouthEast');
            %}
            figure(2)
            plot(plot_q_svm{g,sq},1:length(plot_q_svm{g,sq}),'DisplayName',['SVM score',name]);
            hold on
        end
        figure(2);
        plot(plot_qM,1:length(plot_qM),'DisplayName','Mascot score');
        title({'Zależność liczby prawidłowych identyfikacji';'z bazy target od q-wartości'; ['\fontsize{10}',title_spec]});
        xlabel('q-wartości');ylabel('liczba prawidłowo zidentyfikowanych peptydów');
        set(gca,'XLim',[0 0.2]);
        legend('Location','SouthEast')
        hold off
        fig_spec = [score_method{s},'_q',num2str(q_threshold(q)),'.png'];
        saveas(gcf,[fig_loc,'_qvalues/q_',fig_spec])
        
    end
end

