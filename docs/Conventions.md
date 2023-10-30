# How to structure and name packages, classes, and properties
Since the implementation is based on the [Android app architecture guidelines](https://developer.android.com/topic/architecture) and [clean architecture](https://www.raywenderlich.com/3595916-clean-architecture-tutorial-for-android-getting-started) concepts
the naming and package structure will be based on those, too.

🏗️ &nbsp; The project has 3 main packages:
- **data**
- **domain**
- **presentation**

⚠️ &nbsp; At least data and presentation must have their own data models, and whenever necessary data models could be used in domain layer.

The rest can be configuration or helper packages.


# Kotlin conventions and codestyle
## Use meaningful/descriptive names 

- Try to align with the business requirements example: https://github.com/radixdlt/babylon-wallet-android/pull/598#discussion_r1370672922   

- Avoid repetition in hierarchies and namespaces
```
sealed class WalletMessage {
    data object AuthorizedLoginWalletMessage ❌ => AuthorizedLogin ✅
    data object UnauthorizedLoginWalletMessage ❌ => UnauthorizedLogin ✅
    data object RequestMessageWalletMessage ❌ => RequestMessage ✅
}
```

`WalletMessage.AuthorizedLogin` <- not repetition


## Wrap chained calls

Not in one line all the operators: it HELPS in debug

`val anchor = owner?.firstChild!!.siblings(forward = true).dropWhile { it is PsiComment || it is PsiWhiteSpace }` ❌
```
val anchor = owner
    ?.firstChild!!
    .siblings(forward = true)
    .dropWhile { request->
        request is PsiComment || request is PsiWhiteSpace 
    } ✅
```

## Use named arguments 

Unless the meaning of all parameters is ABSOLUTELY clear from context.


## Parameter list wrapping

Next line when more than two arguments
```
// named arguments & parameter list wraping
drawSquare(
	x = 10,
	y = 10,
	width = 100,
	height = 100,
	fill = true
)
```

## Avoid all usage of the implicit argument name `it` inside multi-line expressions or multi-line lambdas
Unless it is absolutely clear from context.

