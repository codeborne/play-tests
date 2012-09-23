play-selenide
=============

Play framework integration with Selenide (based on Selenium Web Driver) to be able to write UI tests for Play apps in plain Java instead of html files

Add it to your dependencies.yml
-------------------------------

    require:
        - play
        - play-selenide -> selenide 0.1
    
    repositories:
        - play-selenide:
          type: http
          artifact: http://cloud.github.com/downloads/codeborne/play-selenide/[module]-[revision].zip
          contains:
            - play-selenide -> *
