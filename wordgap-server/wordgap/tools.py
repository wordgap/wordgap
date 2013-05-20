import codecs
import sys 
import os
from nltk import PunktSentenceTokenizer
from nltk.tokenize import word_tokenize
from nltk.corpus import wordnet
import nltk.data
import cPickle
import re
import operator
import en 
from collections import defaultdict
import random
#from nltk import ClassifierBasedPOSTagger



# tagger is loaded on import for performance reasons 
fh = None 
try:
    fh = open("wordgap/data/tagger_cpik.pickle", "rb")
    # zum Testen ohne Server: 
    #fh = open("data/tagger_cpik.pickle", "rb")
    tagger = cPickle.load(fh)

except: 
    print("Kann Tagger nicht laden!")        
finally:
    if fh != None:
        fh.close()
# tokenizer is loaded on import for performance reasons 
tokenizer = nltk.data.load('tokenizers/punkt/english.pickle')





# return type: string 
def load_text(filename):
    textraw = ""
    fh = None
    try:
        filesize = os.path.getsize(filename)
        fh = codecs.open(filename, mode="r", encoding="utf-8", errors="strict")
        if filesize <= 20000: 
            textraw = fh.read()
        else:
            textraw = fh.read(20000)

        return textraw

    except UnicodeError: 

        print("no utf-8 file!")
        sys.exit(-1)

    except IOError as err:

        print(str(err))
        sys.exit(-1)

    finally:
        fh.close()

# return type: list of lists (sentences) of strings (tokens)
def tokenize(textraw):



    sents = tokenizer.tokenize(textraw.strip(), realign_boundaries=True)

    if len(sents) == 1:
        return word_tokenize(sents[0])

# zusaetzlich an Absaetzen trennen (erkennt PunktSentenceTokenizer nicht automatisch, warum auch immer)

    sents2 = []  
    for i in range(len(sents)): 

        if "\n\n" in sents[i]:
            splits = sents[i].split("\n\n")
            sents2.extend(splits)
        else:
            sents2.append(sents[i])
    sents = sents2

# Klammern vereinigen (da Abkuerzungen oft innerhalb von Klammern auftreten)

    for i in range(len(sents)):
        if i < (len(sents)-1):
            if sents[i].count("(") > sents[i].count(")") and sents[i+1].count(")") > sents[i+1].count("("):
                sents[i] = sents[i] + sents[i+1]
                sents[i+1] = unicode("")
            
# kurze Saetze rausnehmen
    for i in range(len(sents)):
        w = len(sents[i].split())
        if w > 0 and w < 5:
            if i < (len(sents)-1):
                sents[i] = sents[i] + " " + sents[i+1]        
                sents[i+1] = unicode("")
            else:
                sents[i-1] += " " + sents[i]
                sents[i] = unicode("")

# leere Elemente und Whitespace entfernen 
    sents = [s.strip() for s in sents if s]

# in Token zerlegen mit der vom NLTK empfohlenen Standardfunktion (da tagger darauf aufbaut)
    tokens = [word_tokenize(s) for s in sents]
    return tokens


# return type: list of lists of (token, tag) tuples
def tag(tokens):

    return [tagger.tag(i) for i in tokens]
    

# return type: [lemma, count [[token (original), POS tag, lemma, index des satzes im text, index des tokens im satz], ...] ]
def get_nouns(tokens_tagged):

    # TODO anfangs initialisieren
    # Satzzeichen usw. raus, werden manchmal falsch getaggt 
    r = re.compile(r'[^a-zA-Z]')
    nouns = []
    for i in range(len(tokens_tagged)):
        # new sentence
        s = tokens_tagged[i]
        # for every token in the sentence 
        for j in range(len(s)):
            (w, t) = s[j]
            if t == 'NN' and not r.match(w):
                nouns.append([w, t, unicode(w.lower()), i, j])
            elif t == 'NNS'and not r.match(w):
                nouns.append([w, t, unicode(en.noun.singular(w.lower())), i, j])

    # frequency ermitteln
    count = defaultdict(int)
    for liste in nouns:
        count[liste[2]] += 1
    # nach frequency absteigend sortieren
    sorted_counts = sorted(count.items(), key=operator.itemgetter(1), reverse=True)
    # alle Lemmata, die nicht in Wordnet enthalten sind, entfernen 
    sorted_counts = [(n, c) for (n, c) in sorted_counts if en.is_noun(n)]
    # an alle lemmata die liste ihrer vorkommen anhaengen 
    nouns_all = []
    for (n, c) in sorted_counts:
        liste = [l for l in nouns if l[2]== n]
        nouns_all.append([n, c, liste])

    return nouns_all

# VERBS
def get_verbs(tokens_tagged):

    r = re.compile(r'[^a-zA-Z]')
    verbs = []
    for i in range(len(tokens_tagged)):
        s = tokens_tagged[i]
        for j in range(len(s)):
            (w, t) = s[j]
            if t and t.startswith('V') and not r.match(w):
                verbs.append([w, t, unicode(en.verb.infinitive(w.lower())), i, j])

    count = defaultdict(int)
    for liste in verbs:
        count[liste[2]] += 1
    
    sorted_counts = sorted(count.items(), key=operator.itemgetter(1), reverse=True)
    blocked = ["", "be", "have", "do", "can"]
    sorted_counts = [(w, c) for (w, c) in sorted_counts if en.is_verb(w) and w not in blocked]

    verbs_all = []
    for (w, c) in sorted_counts:
        liste = [l for l in verbs if l[2]== w]
        verbs_all.append([w, c, liste])

    return verbs_all


# ADJ
def get_adj(tokens_tagged):
    
    r = re.compile(r'[^a-zA-Z]')
    adj = []
    for i in range(len(tokens_tagged)):
        s = tokens_tagged[i]
        for j in range(len(s)):
            (w, t) = s[j]
            if t and t.startswith('J') and not r.match(w):
                adj.append([w, t, unicode(w.lower()), i, j])

    count = defaultdict(int)
    for liste in adj:
        count[liste[2]] += 1
    
    sorted_counts = sorted(count.items(), key=operator.itemgetter(1), reverse=True)
    sorted_counts = [(w, c) for (w, c) in sorted_counts if en.is_adjective(w)]

    adj_all = []
    for (w, c) in sorted_counts:
        liste = [l for l in adj if l[2]== w]
        adj_all.append([w, c, liste])

    return adj_all

# d ist dictionary mit index - count werten
# return type: one of the keys of the dictionary
def simple_prob_dist(d):

    i = 1
    d2 = dict()
    for k in d.keys():
        r = (i, i+d[k])
        #print r
        i += d[k]
        #print k
        d2[k] = r

    x = random.randint(1, i-1)
    #print "x: " + str(x)
        
    for k in d2.keys():
        if x in range(*d2[k]):
            chosen = k
            break

    return chosen

# s ist string 
# generische methode in nodebox (wordnet.hyponym usw.) funktioniert nicht bzw doku stimmt nicht 
# daher redundanter code, jeweils per pos tag  
def get_siblings(s, pos='n'):

    if pos == 'n':
        hyper = en.wordnet.flatten(en.noun.hypernym(s))
        hypo = en.wordnet.flatten([en.noun.hyponyms(h) for h in hyper])
        syns = en.noun.senses(s)[0]
    elif pos == 'v':
        hyper = en.wordnet.flatten(en.verb.hypernym(s))
        hypo = [en.verb.hyponyms(h) for h in hyper]
        syns = en.verb.senses(s)[0]
    else: 
        hyper = en.wordnet.flatten(en.adjective.hypernym(s))
        hypo = [en.adjective.hyponyms(h) for h in hyper]
        syns = en.adjective.senses(s)[0]
    if hypo:
        f = lambda x: " " not in x and not x.istitle() and "_" not in x and "-" not in x and x not in syns 
        siblings = set(filter(f, en.wordnet.flatten(hypo)))
        
        return [unicode(i) for i in list(siblings)]
    else:
        return []

    
def get_remote_syns(s, pos='n'):

    if pos == 'n': 
        syns = en.noun.senses(s)
        if syns:
            allsyns = sum(syns, [])
             
            if len(syns) >= 2:
                syns = syns[0] + syns[1]
            else:
                syns = syns[0]
            #print syns 
        else:
            return [] 
        remote = []
        for syn in syns:
            newsyns = en.noun.senses(syn)
            #print "newsyns: " + str(newsyns)
            remote.extend([r for r in newsyns[0] if r not in allsyns and " " not in r and r not in s and s not in r and len(r) >= 3 and r.islower()])

    elif pos == 'v':
        syns = en.verb.senses(s)
        if syns:
            allsyns = sum(syns, [])
            if len(syns) >= 2:
                syns = syns[0] + syns[1]
            else:
                syns = syns[0]
        else:
            return [] 
        remote = []
        for syn in syns: 
            newsyns = en.verb.senses(syn)
            #print newsyns
            remote.extend([r for r in newsyns[0] if r not in allsyns and " " not in r and r not in s and s not in r and len(r) >= 3 ])

    else:  
        syns = en.adjective.senses(s)
        if syns:
            allsyns = sum(syns, [])
            if len(syns) >= 2:
                syns = syns[0] + syns[1]
            else:
                syns = syns[0]
        else:
            return [] 
        remote = []
        for syn in syns: 
            newsyns = en.adjective.senses(syn)
            #print newsyns
            remote.extend([r for r in newsyns[0] if r not in allsyns and " " not in r and "-" not in r and r not in s and s not in r and len(r) >= 3])


    return [unicode(i) for i in list(set(remote))]
    
# adapt distractors grammatically (nouns and verbs) + their capitalisation

def adapt_dis(chosen, pos, dis):

    token = chosen[1]
    tag = chosen[2]
    if pos == 'n':
        if tag == 'NNS':
            dis = [en.noun.plural(d) for d in dis]

    elif pos == 'v':

#VB 	Verb, base form
#VBD 	Verb, past tense
#VBG 	Verb, gerund or present participle
#VBN 	Verb, past participle
#VBP 	Verb, non-3rd person singular present (kann weggelassen werden, da kein unterschied zu VB, ausser to be, welches als 
# zielwort gefiltert wird und als distraktor eher unwahrscheinlich ist.) 
#VBZ 	Verb, 3rd person singular present  	
# key error kann bei zusammengesetzten verben auftreten, daher werden diese bei get_dis ausgeschlossen
# falls das verb dennoch unbekannt ist, koennen die distractors nicht angepasst werden 
        try: 
            if tag == 'VBD':
        
                dis = [en.verb.past(d) for d in dis]

            elif tag == 'VBG':

                dis = [en.verb.present_participle(d) for d in dis]

            elif tag == 'VBN':

                dis = [en.verb.past_participle(d) for d in dis]

            elif tag == 'VBZ':        

                dis = [en.verb.present(d, person=3) for d in dis]

        except: 
            pass

    if token.istitle():
        dis = [d.title() for d in dis]
    return dis 

# get distractors
def get_dis(chosen, pos, lemmas_in_order_of_frequency, fast=False, no_tries=0):

    random.seed()
    lemma = chosen[3]
    dis = ["", "", ""]

    if fast:

        syns = []
        if pos == 'n':
            syns = en.wordnet.flatten(en.noun.senses(lemma))
        elif pos == 'v':
            syns = en.wordnet.flatten(en.verb.senses(lemma))
        elif pos == 'a':
            syns = en.wordnet.flatten(en.adjective.senses(lemma))

        dis[0] = random.choice(lemmas_in_order_of_frequency)
        dis[1] = random.choice(lemmas_in_order_of_frequency)
        dis[2] = random.choice(lemmas_in_order_of_frequency)
        again = False
        for d in dis:
            if d in syns:
                again = True

        if ((len(set(dis)) != len(dis)) or ("" in dis) or (lemma in dis)):
            again = True 

        if again: 
            if no_tries > 10:
                return None  
            else: 
                no_tries += 1
                return get_dis(chosen, pos, lemmas_in_order_of_frequency, fast=fast, no_tries=no_tries)

        else:
            return dis 
    # SLOW 
    else: 
    # antonym wenn moeglich 
        if pos == 'n':
            a = en.wordnet.flatten(en.noun.antonym(lemma))
        elif pos == 'v':
            a = en.wordnet.flatten(en.verb.antonym(lemma))
        elif pos == 'a':
            a = en.wordnet.flatten(en.adjective.antonym(lemma))

        if a:
            #print("antonym gefunden! " + str(a))
            dis[0] = random.choice(a)

        sibs = get_siblings(lemma, pos)
        false_syns = get_remote_syns(lemma, pos)
        candidates = sibs + false_syns
    #print "candidates: " + str(candidates)

        if candidates:
            next = random.choice(candidates)
        else:
            next = random.choice(lemmas_in_order_of_frequency)
    
        dis[1] = next  

        if dis[0] == "":
            if candidates:
                next = random.choice(candidates)
            else:
                next = random.choice(lemmas_in_order_of_frequency)
            dis[0] = next 
    # min ein wort muss immer aus dem kontext des textes kommen 
        dis[2] = random.choice(lemmas_in_order_of_frequency)

    # candidates koennte woerter aus lemmas enthalten oder es wurde mehrmals dasselbe wort aus lemmas gewaehlt
    # oder ein wort entspricht dem lemma 
    # bei kurzen Texten verhindert Parameter no_tries eine Endlosschleife 
        if (len(set(dis)) == len(dis) and "" not in dis and lemma not in dis):
            return dis  

        else:

            if no_tries > 10:
                return None 
    # try again (rekursiv, no_tries wird bei jedem Versuch erhoeht)
            else: 
                no_tries += 1
                return get_dis(chosen, pos, lemmas_in_order_of_frequency, fast=fast, no_tries=no_tries)

# return type: list of (lemma, definition) tuples 
def get_definitions(lemmas_in_order_of_frequency, pos):

    if pos == 'n':
        return [(l, en.noun.gloss(l)) for l in lemmas_in_order_of_frequency]
    elif pos == 'v':
        return [(l, en.verb.gloss(l)) for l in lemmas_in_order_of_frequency]
    elif pos == 'a':
        return [(l, en.adjective.gloss(l)) for l in lemmas_in_order_of_frequency]

def get_token_definitions(lemmas_in_order_of_frequency, pos):

    if pos == 'n':
        return [(en.noun.singular(l.lower()), en.noun.gloss(en.noun.singular(l.lower()))) for l in lemmas_in_order_of_frequency]
    elif pos == 'v':
        return [(en.verb.infinitive(l.lower()), en.verb.gloss(en.verb.infinitive(l.lower()))) for l in lemmas_in_order_of_frequency]
    elif pos == 'a':
        return [(l.lower(), en.adjective.gloss(l.lower())) for l in lemmas_in_order_of_frequency]

# return type: list of lists  
def sanitize_sents(sents_with_cloze):

    temp = []
    first_is_without = False 
    for i in range(len(sents_with_cloze)):
        # saetze ohne uebung an welche mit anhaengen und entfernen 
        # erkennbar daran, dass nur der satz enthalten ist, dh die liste die laenge 1 hat 
        if i == 0:
            temp.append(sents_with_cloze[0])
            if len(sents_with_cloze[0]) == 1:
                first_is_without = True 
        else:
            if len(sents_with_cloze[i]) > 1:
                temp.append(sents_with_cloze[i])
            else:
                if len(temp[-1]) >= 2:
                    temp[-1][1] += " " + sents_with_cloze[i][0]
                else:
                    temp[-1][0] += " " + sents_with_cloze[i][0]

    if first_is_without: 
        # ersten Satz ohne Uebung davorhaengen 
        temp[1][0] = temp[0][0] + " " + temp[1][0]
        del(temp[0]) 

    return temp 

def prepare_text(text):

    sents = tokenize(text)
    tagged = tag(sents)
    return tagged

def test():

    filename = "data/test.txt"
    text = load_text(filename)
    sents = tokenize(text)
    tagged = tag(sents)
    #print len(sents)
    #print get_nouns(tagged)
    #print get_adj(tagged)
    #print get_verbs(tagged)
    return tagged  


