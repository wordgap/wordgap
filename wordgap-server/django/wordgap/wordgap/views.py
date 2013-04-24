from __future__ import division
from django.http import HttpResponse
import json
import codecs
from django.http import Http404
from django.shortcuts import render_to_response
from datetime import datetime
import random
from django.contrib.sites.models import Site
import wordex
import tools 
import prepex 
from django.utils import simplejson 
from os import path, access, R_OK
#from django.core import serializers
 
# /post 
def getjson(request):

    if request.method == "POST":

        data = request.POST
        text = data.get("text")
        pos = data.get("pos")
        if pos == 'p':
            ex = prepex.create_prepex(text)
        else:
            ex = wordex.create_ex(text, pos=pos, fast=True)
        if ex:
            return HttpResponse(simplejson.dumps(ex), mimetype='application/json') 
        else:
            raise Http404
    else: 
        raise Http404


def wordlist(request):

    if request.method == "POST":

        data = request.POST
        wordlist = data.get("wordlist")
        pos = data.get("pos")
        wordlist = wordlist.split('\n\n')
        print wordlist

        print "pos: " + pos 
        if pos == 'p':
            raise Http404
        else:
            definitions = tools.get_token_definitions([w for w in wordlist if w], pos)
        if definitions:
            return HttpResponse(simplejson.dumps(definitions), mimetype='application/json') 
        else:
            raise Http404

    else: 
        raise Http404

def testjson(request):
    
    text = tools.load_text("wordgap/data/test.txt")
    #ex = wordex.create_ex(text, pos='v', fast=True)
    ex = prepex.create_prepex(text)
    return HttpResponse(simplejson.dumps(ex), mimetype='application/json')

def testprepex(request):

    text = tools.load_text("wordgap/data/test.txt")
    ex = prepex.create_prepex(text)
    if ex is None:
        raise Http404

    request.session.flush()
    request.session["ex"]=ex
    request.session["wrong"] = 0
    request.session["right"] = 0

    return render_to_response("templates/template_start.html")
    
# /wordgap/FILENAME/POS 

def testex(request, filename="wordgap/data/test.txt", pos="v", wordnet="wordnet"):
    
# alles in unicode strings 
# [[wordsbefore, wordsafter, token, [dis]], ...]
    if wordnet=='wordnet':
        fast = True
    else:
        fast = False
    
    filename = "wordgap/data/" + filename + ".txt"

    if path.exists(filename) and path.isfile(filename) and access(filename, R_OK):
        text = tools.load_text(filename)
    else:
        text = tools.load_text("wordgap/data/test.txt")
        
    if (pos=='v' or pos=='n' or pos=='a'):
        ex = wordex.create_ex(text, pos=pos, fast=fast)

    elif pos=='p':
        ex = prepex.create_prepex(text)

    else:
        print "invalid POS Tag" 
        return Http404

    # reset session data
    request.session.flush()
    request.session["ex"]=ex
    request.session["wrong"] = 0
    request.session["right"] = 0
    return render_to_response("templates/template_start.html")

def showSentence(request, sentenceNo, choice=-1, merken=None):

#    current_site = Site.objects.get_current()
#    domain = current_site.domain
#    print(current_site)
#    print(domain)
    ex = request.session.get("ex")
    # ein wort wurde schon gewaehlt
    if choice is not -1:

        current_choices = request.session.get("current_choices")
        token = request.session["token"] 
        wordsbefore = request.session["wordsbefore"] 
        wordsafter = request.session["wordsafter"]
        wrongChoices = request.session["wrongChoices"]

        sentenceNo = int(sentenceNo)
        nextSent = str(sentenceNo + 1)
      
        solution = current_choices.index(token)
        if choice == unicode(solution):       
            solved = True
            request.session["right"] = request.session.get("right", 0) + 1

        else:
            solved = False
            wrongChoices.append(choice)
            request.session["wrongChoices"] = wrongChoices
            request.session["wrong"] = request.session.get("wrong", 0) + 1
            
        
        return render_to_response('templates/template_sentence.html', locals())

    solved = False
    if ex:
        sentenceNo = int(sentenceNo)

        if (sentenceNo-1) == 0:
            request.session["started"] = datetime.now()
        try:
            sentence = ex[sentenceNo-1]
            random.seed()
            wordsbefore = sentence[0]
            wordsafter = sentence[1]
            token = sentence[2]
            distractorwords = sentence[3]
            request.session["wrongChoices"] = []
            request.session["token"] = token
            request.session["wordsbefore"] =  wordsbefore
            request.session["wordsafter"] = wordsafter
            current_choices = [token] + distractorwords[:3]
            random.shuffle(current_choices)
            request.session["current_choices"] = current_choices
            nextSent = str(sentenceNo + 1)
            
            return render_to_response('templates/template_sentence.html', locals())

        except IndexError as ie:
            print(str(ie))
            return("<a class=\"reference internal\" href=\"../result\"><em>Ergebnis</em></a>")
    else:
        raise Http404
    return None

def result(request):

    request.session["finished"] = datetime.now()
    if request.session.get("started"):
        # timedelta obj 
        duration = request.session["finished"] - request.session["started"]
        sek = duration.seconds
        stunden, rest = divmod(sek, 3600)
        minuten, sek = divmod(rest, 60)
        print '%sentence:%sentence:%sentence' % (stunden, minuten, sek)


    if request.session.get("right"):
        right = int(request.session["right"])
    else:
        return HttpResponse("Ergebnis nicht verfuegbar")

    if request.session.get("wrong"):
        wrong = int(request.session["wrong"])

    gesamt = right + wrong 
    percent = (right / gesamt) * 100
    percent = int(round(percent))
 
    return render_to_response('templates/template_result.html', locals())
    
        
