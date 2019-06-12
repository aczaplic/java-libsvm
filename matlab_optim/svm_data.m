close all
clear

N = 1;
%wczytanie pelnego zbioru (do testow)
filename = '7bialek.txt';
dataset = importdata(filename);
full_data = dataset.data;
if ~isempty(full_data)
    pos_neg_F = full_data(:,1);
    full_data = full_data(:,2:end);
    full_data = full_data-repmat(min(full_data,[],1),size(full_data,1),1);
    full_data = full_data./repmat(max(full_data,[],1),size(full_data,1),1);
end

pos_idn=zeros(2,3);

%wybor score Mascota
score = full_data(:,7);

%wyznaczenie q-wartosci na podstawie uporzadkowania zgodnego z wybranym score Mascota
qM = get_fdr(~pos_neg_F,score);
plot_qM = sort(qM(pos_neg_F==1));
    
    pos_idn(N+1,1) = min(find(plot_qM>0.01));
    pos_idn(N+1,2) = min(find(plot_qM>0.05));
    pos_idn(N+1,3) = min(find(plot_qM>0.1));


%stworzenie zbioru uczacego
%new_filename = learning_set(filename,0.2,1,'score');
new_filename = '7bialek_0.2_1_score.txt';

%odczyt pliku
new_dataset = importdata(new_filename);
data = new_dataset.data;

if ~isempty(data)
    %pobranie danych i informacji o przynaleznosci do klas
	pos_neg = data(:,1);
	data = data(:,2:end);       
	
	%normalizacja danych
	data = data-repmat(min(data,[],1),size(data,1),1);
	data = data./repmat(max(data,[],1),size(data,1),1);    
end


SVMmodels = cell(size(gamma,2),size(C,2));
 for i = 1:N
    %tworzenie i sprawdzenie modelu SVM
    SVMmodels{g,c} = fitcsvm(Y_train,D_train,'KernelFunction','rbf',...
            'KernelScale',1/sqrt(gamma(g)),'BoxConstraint',C(c),'Cost',penalty,...
            'Standardize',false);
        sv = SVMmodels{g,c}.IsSupportVector;
        nr_sv(g,c) = sum(sv);
       
    %sprawdzenie dzialania modelu na calym zbiorze danych
    [label,SVMscore] = predict(model,full_data);
    error = (sum((label-pos_neg_F).^2))/size(pos_neg_F,1);
    
    %rysowanie histogramu nowego score (wyznaczonego przez siec)
    hist_plot(pos_neg_F,SVMscore(:,2),50,'SVM score');
    
    
    %posortowanie danych zgodnie z nowym score
    [~,ind] = sort(SVMscore(:,2),'descend');
    pos_neg_sort = pos_neg_F(ind);
    %wyznaczenie q-wartosci na podstawie uporzadkownia zgodnego z mscore
    q = get_fdr(~pos_neg_sort);
    
    
    %rysunek zaleznosci liczby identyfikacji z bazy target od q-wartosci (w pełnym zakresie i [0,0.1])
    plot_q_svm = sort(q(pos_neg_sort==1));
    pos_idn(i,1) = min(find(plot_q_svm>0.01));
    pos_idn(i,2) = min(find(plot_q_svm>0.05));
    pos_idn(i,3) = min(find(plot_q_svm>0.1));
    
    figure;
    plot(plot_qM,1:length(plot_qM),plot_q_svm,1:length(plot_q_svm));
    title({'Zależność liczby prawidłowych identyfikacji';'z bazy target od q-wartości'});
    grid;
    xlabel('q-wartości');ylabel('liczba prawidłowo zidentyfikowanych peptydów');
    legend('Mascot score','SVM score','Location','SouthEast');
    
    figure;
    plot(plot_qM,1:length(plot_qM),plot_q_svm,1:length(plot_q_svm));
    title({'Zależność liczby prawidłowych identyfikacji';'z bazy target od q-wartości'});
    grid;
    xlabel('q-wartości');ylabel('liczba prawidłowo zidentyfikowanych peptydów');
    set(gca,'XLim',[0 0.2]);
    legend('Mascot score','SVM score','Location','SouthEast');
 end