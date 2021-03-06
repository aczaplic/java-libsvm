function newfilename=learning_set(filename,q_threshold,use_all_decoy,score_method)

if nargin<4
    score_method='score';       %miara Mascota na podstawie ktorej beda porzadkowane dane podczas wstepnego liczenia q-wartosci
    if nargin<3
        use_all_decoy=1;        %0 -> zapisywanych jest tyle samo negatywnych co pozytywnych, 0 -> zapisywane wszystkie przyklady negatywne
        if nargin<2
            q_threshold=0.1;    %prog q-wartosci, ponizej ktorego dane przypisania z bazy target sa uznawane za prawidlowe
        end
    end
end

%odczyt pliku
dataset = importdata(filename);
data=dataset.data;
sequences=dataset.textdata(2:end,1);
headers=dataset.textdata(1,:);

if ~isempty(data)
    %pobranie danych i informacji o przynaleznosci do klas
	pos_neg=data(:,1);
	data=data(:,2:end);      

    %informacje o pelnym zbiorze danych
    fprintf('PEŁNY ZBIÓR DANYCH:\n');
    fprintf('Licza atrybutów: %d\n',size(data,2));
    fprintf('Licza przykładów pozytywnych (target): %d\n',length(find(pos_neg==0)));
	fprintf('Licza przykładów negatywnych (decoy): %d\n',length(find(pos_neg==1)));

    %wybor miary score
    switch score_method
        case 'score'
            score=data(:,5);
            score_name='score';
        case 'score_mmt'
            score=data(:,5)-min(data(:,6),data(:,7));
            score_name='score-min(MIT, MHT)';
        case 'score_delta'
            score=data(:,8);
            score_name='delta score';
    end
%    hist_plot(~pos_neg,score,50,['Mascot ',score_name,' (pełny zbiór)']);
        
    %posortowanie danych zgodnie z wybranym score    
    [~,ind]=sort(score,'descend');
    data=data(ind,:);
    pos_neg=pos_neg(ind);
    sequences=sequences(ind);
    
    %wyznaczenie q-wartosci na podstawie uporzadkownia zgodnego ze score
	q=get_fdr(pos_neg);

    %rysunek zaleznosci liczby identyfikacji z bazy target od q-wartosci (w zakresie od 0 do 0.2)
% 	figure;
%     plot_q=q(pos_neg==0);
% 	plot(plot_q,1:length(plot_q));
% 	set(gca,'XLim',[0 0.2]);
    
    
	%wyznaczenie przykladow pozytywnych (wszystkie z bazy decoy ponizej progu q_threshold) i negatywnych (wszystkie lub wybrane z bazy decoy, tak aby bylo ich tyle samo ile pozytywnych)
	pos_index=find(pos_neg==0);
	neg_index=find(pos_neg==1);

	pos_index=intersect(pos_index,find(q<=q_threshold));
    if use_all_decoy==0
        neg_index_perm=randperm(length(neg_index))';
		neg_index=neg_index(sort(neg_index_perm(1:min(length(neg_index),length(pos_index)))));
    end
    
    %ostateczny zbior danych uczacy
    index=([pos_index;neg_index]);
    index=index(randperm(length(index)));
    data=data(index,:);
    pos_neg=pos_neg(index);
    sequences=sequences(index);

    
    %zapis stworzonego zbioru do pliku
    new_dataset=table2cell(array2table([[0;pos_neg],[0;pos_neg],[zeros(1,size(data,2));data]]));
    for i=1:size(sequences,1)
        new_dataset{i+1,1}=sequences{i,1};
    end
    for i=1:size(headers,2)
        new_dataset{1,i}=headers{1,i};
    end
    new_dataset=cell2table(new_dataset);
    newfilename=[filename(1:end-4),'_',num2str(q_threshold),'_',num2str(use_all_decoy),'_',score_method,'.txt'];
    writetable(new_dataset,newfilename,'WriteVariableNames',false,'Delimiter','tab');

    
    %informacje o zbiorze uczacycm
    fprintf('ZBIÓR DANYCH UCZĄCYCH:\n');
    fprintf('Licza atrybutów: %d\n',size(data,2));
    fprintf('Licza przykładów pozytywnych (target): %d\n',length(find(pos_neg==0)));
	fprintf('Licza przykładów negatywnych (decoy): %d\n',length(find(pos_neg==1)));
    
%    hist_plot(~pos_neg,score,50,['Mascot ',score_name,' (zbiór uczący)']);
end