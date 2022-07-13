# babylon-wallet-android


# Versioning support

Currently, minimum support version is   Android 8.0 (Oreo) sdk 26 as of 12/July/2022, with average 95% market support across Europe & North America.

We support only handsets, no tablets.

Dark mode support will be provided and implemented as we develop features.


# Architecture

Architecture we have chosen is MVVM and will likely to continue with that. 
We target CLEAN architecture to separate concerns and structure project in to separate layers (UI, domain, data)

To target UI implementation we will be using Jetpack Compose and styles/themes will be Material Design based.

To target REST network implementation we will be using Retrofit.

To target dependency injection we will use Hilt. 


# Testing strategy (Pyramid of UI, Linear Image regression, Unit from top to bottom, etc.)

For the beginning, we developers require to have unit tests as the development process. This will be the only part of the testing pyramid for now. At the later point, UI tests are likely to be introduced. TBC.


# Best practices (Caching, Persistence, Security, etc.)

For the security reason we prefer not to store confidential data (tokens, user password, confidential data fetched from backend) on the phone. If we really need to, they should be encrypted up to the best known standards.

SSL pinning (Certificate or Public Key pinning) would be preferred to secure http data transfer.

Crash analysis tool preferably Firebase 

Automated CI and CD (for now probably supported by Github actions?)

Strict code reviews, ideally followed by manual smoke test

No change should be committed without ticket associated with it

As less third party dependencies as possible

Standard gitflow will be used to organise git work (https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)
