# babylon-wallet-android
An Android wallet for interacting with the Radix DLT ledger.

## App requirements
- [Android 8.1](https://developer.android.com/about/versions/oreo/android-8.1) minimum Android version
- Support of handsets and no tablets.

## Architecture and tech stack ⚙️
The architecture is based on the [Android app architecture](https://developer.android.com/topic/architecture) which follows some concepts of the [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html).
So first, please read this one! :) 
In short for architecture:
- Data and presentation layers are the must layers of the app.
- Each of this layer contains its own data models and then we map from one to the other.
- In general we add logic inside the data/domain/presentation model (e.g. `FungibleResource`) only if it is relevant.
- The data layer uses the [repository pattern](https://developer.android.com/static/codelabs/basic-android-kotlin-training-repository-pattern/img/69021c8142d29198.png).
- Domain layer can have its own data models if it is needed. (e.g. to manipulate the data from the data layer, or to combine two or more different data models from the data layer to one in the domain layer.)
- Use cases (they live in domain layer) can provide real benefits in some scenarios, please see 4 bullets [here](https://developer.android.com/topic/architecture/domain-layer). Another example of a use case is the `ShowOnboardingUseCase`.
- MVVM for the presentation layer

🏗️ The tech stack:
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for UI
- [Material 3](https://m3.material.io/) it is actually enabled but not correctly/heavily used. (Dark mode support will be provided and implemented as we develop features.)
- [Retrofit](https://square.github.io/retrofit/) + [okhttp](https://square.github.io/okhttp/) for REST
- [Ktor](https://ktor.io/) for websockets
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) for DI
- [Coil](https://coil-kt.github.io/coil/) for image loading
- [Detekt](https://detekt.dev/) + plugins (formatting, [compose rules](https://twitter.github.io/compose-rules/)) for static code analysis
- [JaCoCo](https://www.eclemma.org/jacoco/) for code coverage

We try to stay with native libraries as much as possible. As less third party dependencies as possible.

🔗 Dependencies
- we use shared libraries version catalog located in `libraries.versions.toml`
- to update all dependencies in catalog please run `gradle versionCatalogUpdateLibraries`
- to have control over what is updated please run `gradle versionCatalogUpdateLibraries --interactive` which will generate version diff file. You can inspect what will be updated, and you can remove libraries that you want to exclude from the update. To apply the diff, run `gradle versionCatalogApplyUpdatesLibraries`

## Useful tips/resources and best practices (IMPORTANT) 💡
- [Keep It Simple](https://imageio.forbes.com/specials-images/imageserve/6141f431cb79cea26593300b/Shortcut-From-Point-A-to-Point-B-Concept/960x0.jpg?format=jpg&width=960)
- Write your code, leave it aside for one week, come back and read it. If you don't get what your code does in less than a minute, then probably you overengineering!
- Comments are helpful. :) 
- Watch this [video](https://www.youtube.com/watch?v=OMPfEXIlTVE), really! It helps you understand when to (not) use abstraction, inheritance, and when to use composition + DI.
- Read the [conventions](https://github.com/radixdlt/babylon-wallet-android/blob/main/docs/Conventions.md) doc

Some useful Kotlin resources
- [Value classes](https://quickbirdstudios.com/blog/kotlin-value-classes/)
- [Sealed interfaces](https://quickbirdstudios.com/blog/sealed-interfaces-kotlin/ )

This project is tested with BrowserStack.

## License

The Android Radix Wallet binaries are licensed under the [Radix Wallet Software EULA](https://www.radixdlt.com/terms/walletEULA).

The Android Radix Wallet code is released under the [Apache 2.0 license](./LICENSE).


      Copyright 2023 Radix Publishing Ltd

      Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

      You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

      See the License for the specific language governing permissions and limitations under the License.


