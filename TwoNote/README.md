# TwoNote Graph Sample
This sample is a note taking app that uses the [Microsoft Graph](https://learn.microsoft.com/graph/overview) to sync a user's notes to OneNote.

This is built from a note taking sample published in the [Surface Duo Window Manager Samples](https://github.com/microsoft/surface-duo-window-manager-samples) repo called TwoNote.

## Getting Started

Before building the project, you will need the following:

 * [Android Studio](https://developer.android.com/studio/) installed on your development machine.
 * Either a [personal Microsoft account](https://account.microsoft.com/account) (MSA), or a Microsoft work or school account (AAD).

## Configure Your Project

To call the Graph API in an Android app, an authentication token associated with a signed in user is required as part of the request. There are two parts required to retrieve this token: 
 1. Register your app in the [Azure Portal](https://ms.portal.azure.com/)
 2. Set up the configuration files in your Android project.

If you are unfamiliar with the process of registering an Android app in the Azure Portal and using the Microsoft Authentication Library (MSAL) for Android, consider reading the [Android Graph API tutorial](https://learn.microsoft.com/azure/active-directory/develop/tutorial-v2-android) or downloading a tutorial app and running through the [Android quickstart](https://learn.microsoft.com/azure/active-directory/develop/mobile-app-quickstart?pivots=devlang-android).

### 1. Register an app in the Azure Portal

Follow the [app registration guidelines](https://learn.microsoft.com/en-us/azure/active-directory/develop/tutorial-v2-android#register-your-application) to set up your app. There are a few configurations specific to this sample we will be making:
 * When creating a **New Registration**, make sure to select **Accounts in any organizational directory (Any Azure AD directory - Multitenant) and personal Microsoft accounts (e.g. Skype, Xbox)** from the Supported account types selection.
 * When creating a new Redirect URI, make sure to include the correct **Package name**. For this sample, the value is `com.example.graphprototype`
     * Finding the correct **Signature hash** can be tricky. An alternative to downloading all the tools needed to generate a hash is to skip this step and run through the rest of the configuration process. Once finished, build the project in Android Studio. Android Studio should give an error or debug message about what signature has it was expecting. Copy this value and enter it into the Azure portal and Android config files.
 * Make sure to set the correct **API permissions** in the Azure portal. You will need to add [Microsoft Graph permissions](https://learn.microsoft.com/en-us/graph/permissions-reference)
     * `Notes.Create`
     * `Notes.Read`
     * `Notes.ReadWrite`
     * `User.Read`

### 2. Edit configuration files in Android Studio
Clone or fork this repo and follow the [configuration guidelines](https://learn.microsoft.com/en-us/azure/active-directory/develop/tutorial-v2-android#configure-your-application) to set up your project in Android Studio. The template should be set up and ready, all you need to change are the following:
 * The **client_id** from your app registration in the Azure Portal needs to be placed in [auth_config_single_account.json](app/src/main/res/raw/auth_config_single_account.json).
 * The **redirect_uri** from your app registration in the Azure Portal needs to be placed in [auth_config_single_account.json](app/src/main/res/raw/auth_config_single_account.json).
 * The **redirect_uri** needs to also be placed in the [AndroidManifest.xml](app/src/main/AndroidManifest.xml). It is formatted a bit differently in this file, but all the pieces are still there. All that needs to be changed is updating the `android:path` value to whatever your **Signature hash** should be.

## Using the App

To see more information about the sample app and how it interacts with the Microsoft Graph, see the [App Description](docs/APP_DESCRIPTION.md) documentation.

## Known Issues

There are a couple of outstanding issues with the Graph SDK that we have filed and are monitoring for improvements:
 * ["496 Error during http request" when processing response in Android app](https://github.com/microsoftgraph/msgraph-sdk-java/issues/1343)
 * ["Update OneNote page content not working - incorrect url schema being used"](https://github.com/microsoftgraph/msgraph-sdk-java/issues/1361)

