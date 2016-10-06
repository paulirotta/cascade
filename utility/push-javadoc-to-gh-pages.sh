#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "paulirotta/cascade" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then

  ./gradlew generateReleaseJavadoc --info
  echo -e "Publishing javadoc...\n"

  cp -R cascade/build/docs/javadoc $HOME/javadoc-latest

  cd $HOME
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"
  git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/paulirotta/cascade gh-pages 

  cd gh-pages
  git rm -rf ./javadoc
  cp -Rf $HOME/javadoc-latest ./javadoc
  git add -f .
  git commit -m "Latest javadoc on successful travis build $TRAVIS_BUILD_NUMBER auto-pushed to gh-pages"
  git push -fq origin gh-pages

  echo -e "Published Javadoc to gh-pages.\n"
  
fi