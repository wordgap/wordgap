import tools
import re 
import en 
import random 

def create_ex(text, pos='n', last_index=False, fast=False):

    # liste mit liste von token 
    # sentence und word tokenizing 
    sents = tools.tokenize(text)
    # POS tagging 
    tagged = tools.tag(sents)

    # [lemma, count [[token (original), POS tag, lemma, index des satzes im text, index des tokens im satz], ...] ]
    if pos == "n":
        words = tools.get_nouns(tagged)
    elif pos == "v":
        words = tools.get_verbs(tagged)
    elif pos == 'a':
        words = tools.get_adj(tagged)
    else:
        print("Fehler: Unbekannter POS Tag!")
        return
    lemmas_in_order_of_frequency = [k[0] for k in words]
    print len(lemmas_in_order_of_frequency)
    # no of sentences available
    sent_count = len(tagged)
    sents_with_cloze = [[] for x in xrange(sent_count)]
    # regex, die mit look-ahead nach Whitespace vor bestimmten Satzzeichen sucht 
    r = re.compile(r'\s(?=,|\.|!|;|"|\'|\))')

    for i in range(sent_count):
        # satz vorbereiten 
        s = sents[i]
        
        # welche lemmata kommen in diesem satz vor? 
        lemmas_in_s = []
        for n in words:
            for k in n[2]:
                if k[3]==i:
                    lemmas_in_s.append([n[1]] + k)
        
        # [[10, u'prince', 'NN', u'prince', 1, 3], [4, u'flowers', 'NNS', u'flower', 1, 7], ...]
        # wenn aus dem satz keine cloze question gebildet werden kann: 
        if lemmas_in_s == []:
            # leerzeichen vor , . ! usw. entfernen, dass durch " ".join() dazugekommen ist 
            s = re.sub(r, "", " ".join(s))
            sents_with_cloze[i].append(s)
            continue 

        elif len(lemmas_in_s) == 1:
            chosen = lemmas_in_s[0]
            
        else:
            # schnellere Alternative: token moeglichst weit hinten im satz, da laut literatur bessere multiple-choice frage 
            if last_index:
                #print lemmas_in_s 
                last_index = max([k[5] for k in lemmas_in_s])
                chosen_l = [k for k in lemmas_in_s if k[5] == last_index]
                chosen = chosen_l[0]
                #print chosen 
            else:
            # haeufige lemmata bevorzugen damit sie besser gelernt werden 
            # index in lemmas_in_s and frequency of lemma in text 
                indexes_and_counts = dict((k, lemmas_in_s[k][0]) for k in range(len(lemmas_in_s)))
                chosen_index = tools.simple_prob_dist(indexes_and_counts)
                chosen = lemmas_in_s[chosen_index]
                
        token = chosen[1]    
        token_index = chosen[5]

        # WESENTLICH schnellere variante (parameter fast=true) : nur lemmata aus dem text verwenden, erspart zugriffe auf wordnet 
        dis = tools.get_dis(chosen, pos, lemmas_in_order_of_frequency, fast=fast)

        # Wenn keine gueltigen Distraktoren gefunden werden koennen, Satz auslassen 
        if dis == None:
            s = re.sub(r, "", " ".join(s))
            sents_with_cloze[i].append(s)
            continue
        # Distraktoren grammatikalisch anpassen 
        dis = tools.adapt_dis(chosen, pos, dis)


        # a / an angleichen um keine unnoetigen hinweise zu geben 
        if pos == 'n':
            if s[token_index -1] == 'a' or s[token_index -1] == 'an':
                s[token_index -1] = ""
                token = en.noun.article(token)
                dis = [en.noun.article(d) for d in dis]

        wordsbefore = " ".join(s[:token_index])
        if token_index < len(s) - 1:
            wordsafter = " ".join(s[(token_index+1):])
        else:
            wordsafter = ""
        # leerzeichen vor , . ! usw. entfernen, dass durch " ".join() dazugekommen ist 
        wordsbefore = re.sub(r, "", wordsbefore)
        wordsafter = re.sub(r, "", wordsafter)
        cloze = [wordsbefore, wordsafter, token, dis]
        sents_with_cloze[i] = cloze



    # [[wordsbefore, wordsafter, (unicode strings), token (unicode string), [als unicode distractors]], ...]
    
    sents_with_cloze = tools.sanitize_sents(sents_with_cloze)

    return sents_with_cloze


def test():

    from nltk.corpus import gutenberg
    emma = gutenberg.raw('austen-emma.txt')
    print len(emma)
    ex = createexercise(emma, pos='v', last_index=False, fast=True)
    print len(ex)
