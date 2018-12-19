# MVP - Coroutines 

The goal of this project is to show a simple application which uses the MVP pattern with the android `ViewModel` library in order to retain the instance state for its provider.

The application simply downloads successively the summary of a random article from Wikipedia, and displays it along with the thumbnail.

## State Restoration Lifecycle

The goal is to maintain ongoing tasks across configuration changes without reacreating them, and to restore the the result of these tasks whenever the activity is destroyed and recreated. 
