# arbility
![arbility logo](https://github.com/osoc-es/arbility/blob/master/app/src/main/res/drawable/name.png)

To clone the repository execute the command
`git clone https://github.com/osoc-es/arbility.git`

As the project uses the mapbox API you will need to get an API KEY for student map functionality. To do this follow these steps:
1. Sign in or sign up to [MapBox](https://account.mapbox.com/auth/signin/)
2. Go to the *Tokens* section and copy the Default public token
3. Go to \app\src\main\java\com\osoc\oncera\ItineraryMapActivity.java in the project and change YOUR_TOKEN for the token you copied from MapBox

In order to use Firebase in Android Studio go to Tools > Firebase > Authentication. 

It is necessary to upload to Firebase database the file `resouces\obstaclesStandards.json` as the app uses it to check accessibility.


