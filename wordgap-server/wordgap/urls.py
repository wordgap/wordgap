
from django.contrib.staticfiles.urls import staticfiles_urlpatterns
from django.conf.urls import patterns, include, url
from django.http import HttpResponseRedirect
from wordgap.views import *
import settings


# Uncomment the next two lines to enable the admin:
from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',

    (r"^testjson/$", testjson),
    (r"^wordlist/$", wordlist),
    (r"^post/$", getjson),
    (r"^wordgap/$", testex), 
    (r"^wordgap/(?P<filename>\w+)/(?P<pos>[a-z]{1})/(?P<wordnet>\w*)/$", testex),

    (r"^wordgap/result/$", result),
    (r"^wordgap/(?P<sentenceNo>\d+)/$", showSentence),
    #(r"^wordgap/(?P<sentenceNo>\d+)/(?P<merken>merken)/$", showSentence),
    (r"^wordgap/(?P<sentenceNo>\d+)/(?P<choice>\d{1})/$", showSentence),
    (r"^wordgap/(?P<sentenceNo>\d+)/(\d{1}/)*(?P<choice>\d{1})/$", showSentence),

    (r"^wordgap/result/$", result),

    (r"^ex/$", testex),
    (r"^ex/(?P<sentenceNo>\d+)/$", showSentence),
    #(r"^ex/(?P<sentenceNo>\d+)/(?P<merken>merken)/$", showSentence),
    (r"^ex/(?P<sentenceNo>\d+)/(?P<choice>\d{1})/$", showSentence),
    (r"^ex/(?P<sentenceNo>\d+)/(\d{1}/)*(?P<choice>\d{1})/$", showSentence),

    (r"^ex/result/$", result),


    # root
    (r"^$", lambda x: HttpResponseRedirect('/ex')),
    # Examples:
    # url(r'^$', 'wordgap.views.home', name='home'),
    # url(r'^wordgap/', include('wordgap.foo.urls')),

    # Uncomment the admin/doc line below to enable admin documentation:
     url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
     url(r'^admin/', include(admin.site.urls)),
    #(r'^static/(?P<path>.*)$', 'django.views.static.serve',{'document_root': settings.MEDIA_ROOT}),
    (r'^img/(?P<path>.*)$', 'django.views.static.serve',  {'document_root': settings.MEDIA_ROOT}),

    (r'^static/img/(?P<path>.*)$', 'django.views.static.serve',
        {'document_root': '/static', 'show_indexes': True}),
)
# for static files like images 
urlpatterns += patterns('',
    (r'^site_media/(?P<path>.*)$', 'django.views.static.serve',
    {'document_root': settings.MEDIA_ROOT,
    'show_indexes' : True}),
)


