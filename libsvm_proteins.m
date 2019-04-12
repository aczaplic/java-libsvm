function [SVMmodels, SVMscore, plot_q_svm, nr_sv, error, error_train, pos_idnM, pos_idn] = libsvm_proteins()
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

gamma = [10,20,50,100,1000];%[0.01,0.05,0.1,0.5,1,2,4];
%gamma = 0.5;
C = [0.1,1,10,50,100];%,500,1e3];
%C = 10;
%penalty = [0,1;1,0];%sum(D_train)/(size(D_train,1)-sum(D_train));1,0];

pos_idn=zeros(size(gamma,2),size(C,2),3);

%wybor score Mascota
score = full_data(:,7);

%wyznaczenie q-wartosci na podstawie uporzadkowania zgodnego z wybranym score Mascota
qM = get_fdr(~pos_neg_F,score);
plot_qM = sort(qM(pos_neg_F==1));
pos_idnM(1) = min(find(plot_qM>0.01));
pos_idnM(2) = min(find(plot_qM>0.05));
pos_idnM(3) = min(find(plot_qM>0.1));

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
SVMscore = cell(size(gamma,2),size(C,2));
plot_q_svm = cell(size(gamma,2),size(C,2));
nr_sv = zeros(size(gamma,2),size(C,2));
error = zeros(size(gamma,2),size(C,2));
error_train = zeros(size(gamma,2),size(C,2));
for g = 1:size(gamma,2)
    fprintf('gamma = %d', gamma(g));
    for c = 1:size(C,2)
        %tworzenie i sprawdzenie modelu SVM
        options = ['-s 0 -t 2 -g ',num2str(gamma(g)),'-c ',num2str(C(c))];
        SVMmodels{g,c} = svmtrain(pos_neg,sparse(data),options);
        sv = SVMmodels{g,c}.sv_indices;
        nr_sv(g,c) = size(sv,1);
        
        [label, ~, SVMscore{g,c}] = svmpredict(pos_neg, sparse(data), SVMmodels{g,c});
        error_train(g,c) = (sum((label-pos_neg).^2))/size(pos_neg,1);
        
        %sprawdzenie dzialania modelu na calym zbiorze danych
        [label, ~, SVMscore{g,c}] = svmpredict(pos_neg_F, sparse(full_data), SVMmodels{g,c});
        error(g,c) = (sum((label-pos_neg_F).^2))/size(pos_neg_F,1);
        
        %rysowanie histogramu nowego score (wyznaczonego przez siec)
        %hist_plot(pos_neg_F,SVMscore,50,'SVM score');
        
        %posortowanie danych zgodnie z nowym score
        [~,ind] = sort(SVMscore{g,c},'descend');
        pos_neg_sort = pos_neg_F(ind);
        %wyznaczenie q-wartosci na podstawie uporzadkownia zgodnego z mscore
        q = get_fdr(~pos_neg_sort);
        
        %do wyrysowania zaleznosci liczby identyfikacji z bazy target od q-wartosci (w pełnym zakresie i [0,0.1])
        plot_q_svm{g,c} = sort(q(pos_neg_sort==1));
        pos_idn(g,c,1) = min(find(plot_q_svm{g,c}>0.01));
        pos_idn(g,c,2) = min(find(plot_q_svm{g,c}>0.05));
        pos_idn(g,c,3) = min(find(plot_q_svm{g,c}>0.1));
    end
end

%{
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
%}
