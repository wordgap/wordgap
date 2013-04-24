import nltk.data
import codecs
import os
import sys
import logging
from nltk import * 
import tools 
import pickle
import random
import json 
import re 
from pickle import PickleError

# create Exercise object with prepositions eliminated and other prepositions as distractor words
def create_prepex(text):

    sentences = tools.tokenize(text)
    preps_list = []

    # load pickle file with all common preposition from brown corpus 
    try:
        fh_preps = open("wordgap/data/preps_list.pickle", "rb")
        preps_list = pickle.load(fh_preps)
        #print preps_list
    except (PickleError, IOError) as err:     
        print(str(err))    
        return None 
    sents_with_cloze = [[] for x in xrange(len(sentences))]
    # prepositions with four or less letters
    short_preps = preps_list[:21]
    # prepositions with five or more letters
    long_preps = preps_list[22:]
    # sentences is a list of list of strings
    # random initialisieren
    random.seed()
    r = re.compile(r'\s(?=,|\.|!|;|"|\'|\))')

    for i in range(len(sentences)): 
        s = sentences[i]
        index_prep_list = []
        index_sent = []
        cloze = []
     # collect all prepositions in the sentence and their indexes 
        for w in s:
            if w.lower() in preps_list:
                index_prep_list.append(preps_list.index(w.lower()))
                index_sent.append(s.index(w))
                print(w)
     # any preps found?
        if len(index_prep_list) == 0:
            s = re.sub(r, "", " ".join(s))
            sents_with_cloze[i].append(s)
            continue
     # index of the chosen prep. in the sentence
        chosenprep_index = random.choice(index_sent)
     # 'of' nur, wenn es keine andere prep gibt, da zu haeufig 
#        while (s[chosenprep_index] == u"of" and set(index_prep_list) != set([u"of"])):
#            chosenprep_index = random.choice(index_sent)
     # str of the chosen prep.
        chosenprep = s[chosenprep_index]  
     # words before
        wordsbefore = s[:chosenprep_index]
     # words after
        wordsafter = s[chosenprep_index+1:]
        token = chosenprep
     # choose 5 random prepositions as distractors 
        dis = ["", "", ""]
        while True:
            if len(chosenprep) <= 4:
                dis[0] = random.choice(short_preps)
                dis[1] = random.choice(short_preps)
                dis[2] = random.choice(short_preps)
            else:
                dis[0] = random.choice(long_preps)
                dis[1] = random.choice(long_preps)
                dis[2] = random.choice(long_preps)               
            if (chosenprep.lower() not in dis) and (len(dis) == len(set(dis))):
                break 
        # adapt capitalisation
        if chosenprep.istitle():
            dis = [d.capitalize() for d in dis]

        wordsbefore = " ".join(s[:chosenprep_index])
        if chosenprep_index < len(s) - 1:
            wordsafter = " ".join(s[(chosenprep_index+1):])
        else:
            wordsafter = ""
        # leerzeichen vor , . ! usw. entfernen, dass durch join dazugekommen ist 
        wordsbefore = re.sub(r, "", wordsbefore)
        wordsafter = re.sub(r, "", wordsafter)
        cloze = [wordsbefore, wordsafter, token, dis]
        sents_with_cloze[i] = cloze
    
    sents_with_cloze = tools.sanitize_sents(sents_with_cloze)
# [[wordsbefore, wordsafter, (unicode strings), token (unicode string), [als unicode distractors]], ...]
    #print sents_with_cloze
    return sents_with_cloze
#def loadText():
#    text_raw = ""
#    fh = None
#    try:
#        filesize = os.path.getsize("data/test.txt")
#        fh = codecs.open("data/test.txt", mode="r", encoding="utf-8", errors="strict")
#        if filesize <= 20000: 
#            text_raw = fh.read()
#        else:
#            text_raw = fh.read(20000)
#        logging.info("text read")
#        return text_raw

#    except UnicodeError: 
#        #logger.error("no utf-8 file! application will exit now")
#        print("no utf-8 file!")
#        sys.exit(-1)

#    except IOError as err:
#        #logger.error("file not found! application will exit now")
#        print(str(err))
#        sys.exit(-1)

#    finally:
#        fh.close()
    

# MAIN

#def createTestEx():
    #TAG = "createexercise"
    #loggingfile =TAG + ".log"
# empty log file before logging
    #open(loggingfile, "w").close()
# standard logging config
    #logging.basicConfig(filename=loggingfile, level=logging.DEBUG,)

# Text laden aus txt-Datei
 #   textraw = ""
  #  textraw = loadText()
    #logging.info("Text: " + textraw)
    #print(textraw.encode("ascii", "ignore"))

#def initiateLogging():

#    handler = logging.FileHandler("logfile.txt", "w",
#                                  encoding = "UTF-8")
#    formatter = logging.Formatter("%(message)s")
#    handler.setFormatter(formatter)
#    logger = logging.getLogger()
#    logger.addHandler(handler)
#    logger.setLevel(logging.INFO)




    
    
