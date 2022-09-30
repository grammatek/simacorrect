# Símacorrect - Réttritun fyrir Android

## Introduction

Réttritun is a spell checker that provides spell and grammar correction for Icelandic on Android devices.
Spell checking is made via API calls by utilizing the web service [Grammatek Yfirlestur](https://yfirlestur.grammatek.com/), which itself wraps the spelling and grammar checking engine
[GreynirCorrect](https://github.com/mideind/GreynirCorrect).


Réttritun is available on the [Playstore](https://play.google.com/store/apps/details?id=org.grammatek.simacorrect)

Note for Samsung users. The spell checker is not compatible with the default keyboard for Samsung devices [Samsung Keyboard](https://play.google.com/store/apps/details?id=com.samsung.emojikeyboard.themes&hl=is&gl=US). You can switch to using other keyboards that support 3rd party spell checkers, such as Google's keyboard [Gboard](https://play.google.com/store/apps/details?id=com.google.android.inputmethod.latin&hl=is&gl=US).
We recommend though using AnySoftKeyboard [AnySoftKeyboard](https://play.google.com/store/apps/details?id=com.menny.android.anysoftkeyboard&hl=is&gl=US) as it supports 3rd party spell checkers and also makes available an Icelandic language pack developed by us.
If you are able to compile and install AnySoftKeyboard yourself, we have created a fork of AnySoftKeyboard that improves the Icelandic language pack and adds powerful Icelandic next-word completion to it. You can find our fork in the branch `abn-autocompletion` [here](https://github.com/grammatek/AnySoftKeyboard/tree/abn-autocompletion).

## Features
- #### Supports correction for grammatical errors for Android version 12.0 and higher (blue annotation)
    - This covers grammar, compound, punctuation and phrasing errors as well as spelling suggestions.
- #### Correction for spelling errors (red annotation)
    - This covers spelling, capitalization and abbreviation errors.


## Screenshots

<p float="left">
  <img src=".github/images/Google Pixel 4 XL Screenshot 0.png" width = 150/>
  <img src=".github/images/Google Pixel 4 XL Screenshot 1.png" width = 150/>
  <img src=".github/images/Google Pixel 4 XL Screenshot 2.png" width = 150/>
  <img src=".github/images/Google Pixel 4 XL Screenshot 3.png" width = 150/>
  <img src=".github/images/Google Pixel 4 XL Screenshot 4.png" width = 150/>
</p>


## Contributing

You can contribute to this project by forking it, creating a branch and opening a new
[pull request](https://github.com/grammatek/simacorrect/pulls).

## License

Copyright © 2022 Grammatek ehf.


This software is developed under the auspices of the Icelandic Government 5-Year Language Technology Program, described in
[Icelandic](https://www.stjornarradid.is/lisalib/getfile.aspx?itemid=56f6368e-54f0-11e7-941a-005056bc530c) and
[English](https://clarin.is/media/uploads/mlt-en.pdf)

This software is licensed under the [Apache License](LICENSE)

## Acknowledgements
https://github.com/hinrikur/gc_wagtail

