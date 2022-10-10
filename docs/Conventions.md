# How to structure and name packages, classes, and properties
Since the implementation is based on the [Android app architecture guidelines](https://developer.android.com/topic/architecture) and [clean architecture](https://www.raywenderlich.com/3595916-clean-architecture-tutorial-for-android-getting-started) concepts
the naming and package structure will be based on those, too.

🏗️ &nbsp; The project has 3 main packages:
- **data**
- **domain**
- **presentation**

⚠️ &nbsp; At least data and presentation must have their own data models, and whenever necessary data models could be used in domain layer.

The rest can be configuration or helper packages.

## Package structure and name of packages and classes
Let's see the structure and names per layer.
### domain package
- **repositories** package
  - suffix: `Repository`, e.g. _PriceRepository_, _AccountRepository_
- **models** package
  - no suffix! e.g. _Account_, _Price_
- **usecases** package
  - suffix: `UseCase`, e.g. _GetAllAccountsUseCase_

### data package
- **network** package
  - structure packages by feature and each package contains APIs, models and client implementation, e.g. _account_, _price_
    - Client implmentation suffix: `Client`, e.g. _AccountClient_, _PriceClient_
    - Model suffix: `Dto`, (if more than one data model, then create a `dtos` package)
      - _AccountDto_
      - _AccountsDto_
    - APIs suffix: `Api`, e.g. _AccountApi_
  - another feature package: _WebRtcClient_ that contains:
    - _SocketClient_
    - _WebRtcConnector_
    - …
- **localstorage** package _(usually DataStore, SharedPreferences, or in-memory storage)_
  - suffix like `Manager` , e.g. _SharedPreferencesManager_, _DataStoreManager_ if those classes have a general purpose or
  - suffix like `DataSource`, e.g. _UserAccountDatasource_
  - Model suffix: `Entity`, e.g. _UserAccountEntity_ (if more than one data model, then create a `models` package)
- **db** package
  - structure packages by feature and each package contains DAOs, Database classes, models, e.g. _account_
    - DAOs suffix: `Dao`, e.g. _AccountDao_
    - Database suffix: `Database`, e.g. _AccountDatabase_
    - Model suffix: `Entity`, e.g. _AccountEntity_ if more than one data model, then create a `entities` package
- **repositories** package
  - it should contain only the implementations of the repositories
    - Repository implementation suffix: `RepositoryImpl`, e.g. _AccountRepositoryImpl_, _PriceRepositoryImpl_

### presentation package
This can be structured by feature + the ui, utils, helpers, classes/packages. Each feature package contains viewmodels, composables, ui models, and
classes that are related to the feature! For example:
- _Account_
  - _AccountViewModel_, viewmodel suffix: `ViewModel`
  - _AccountScreen_, main screen suffix: `Screen`
  - _ListOfAccountsContent_ (composable that implements a list of accounts)
  - _AccountUi_, presentation model suffix: `Ui`


## Name of variables
### presentation package
- Name the properties of the data classes with their actual name. For example, an _account_ has the following properties: _name_, _address_, _phone_. Then the the `AccountUi` data model has the properties `name`, `address`, `phone`. Avoid the names `accountName`, `accountAddress`, `accountPhone`.
- The (main) state of the composables can be named as `state`. If more than one state is needed in the composables e.g. the scroll state then this specfic one can be named as `scrollState`.


# Kotlin codestyle
Some tips and style recommendations to make Kotlin more understandable.
- For 2 or more constructor arguments, prefer not to keep the properties on the same line as the class name in the constructor definition but as a stack of parameters.
- Avoid all usage of the implicit argument name `it` inside multi-line expressions or multi-line lambdas.
- Specify a name instead of the implicit argument name it inside map, flatMap, switchMap, flatMapLatest and so on lambda argument, and in any function that follows it.
- Use named arguments for lambda arguments, if 2 or more lambdas are being passed to a function.
- Specify the return type of a function, if the function is public (and isn’t coming from `override` of an interface function).
