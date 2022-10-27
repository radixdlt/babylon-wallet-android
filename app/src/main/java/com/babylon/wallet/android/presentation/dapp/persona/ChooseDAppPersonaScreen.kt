// TODO this might be reverted later on
// package com.babylon.wallet.android.presentation.dapp.persona
//
// import androidx.compose.foundation.Image
// import androidx.compose.foundation.layout.Column
// import androidx.compose.foundation.layout.Spacer
// import androidx.compose.foundation.layout.fillMaxSize
// import androidx.compose.foundation.layout.fillMaxWidth
// import androidx.compose.foundation.layout.height
// import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.layout.size
// import androidx.compose.foundation.rememberScrollState
// import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.foundation.verticalScroll
// import androidx.compose.material.Button
// import androidx.compose.material.ButtonDefaults
// import androidx.compose.material.Icon
// import androidx.compose.material.IconButton
// import androidx.compose.material.MaterialTheme
// import androidx.compose.material.Text
// import androidx.compose.material.TextButton
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.ArrowBack
// import androidx.compose.material.icons.filled.Clear
// import androidx.compose.runtime.Composable
// import androidx.compose.runtime.LaunchedEffect
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip
// import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.layout.ContentScale
// import androidx.compose.ui.res.painterResource
// import androidx.compose.ui.res.stringResource
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.tooling.preview.Preview
// import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp
// import coil.compose.rememberAsyncImagePainter
// import com.babylon.wallet.android.R
// import com.babylon.wallet.android.data.dapp.PersonaEntityUiState
// import com.babylon.wallet.android.data.profile.PersonaEntity
// import com.babylon.wallet.android.domain.dapp.DAppState
// import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
// import com.babylon.wallet.android.presentation.dapp.DAppViewModel
// import com.babylon.wallet.android.presentation.dapp.custom.DAppPersonaCard
// import com.babylon.wallet.android.presentation.navigation.Screen
// import com.babylon.wallet.android.presentation.ui.theme.RadixBackground
// import com.babylon.wallet.android.presentation.ui.theme.RadixButtonBackground
// import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2
// import com.babylon.wallet.android.presentation.ui.theme.White
//
// @Composable
// fun ChooseDAppPersonaScreen(
//    viewModel: ChooseDAppPersonaViewModel,
//    onBackClick: () -> Unit,
//    onContinueClick: (Screen) -> Unit,
// ) {
//
//    viewModel.personasState.let { personasState ->
//        personasState.personas?.let { personas ->
//            ChooseDAppPersonaContent(
//                onBackClick = onBackClick,
//                onContinueClick = {
//                    personasState.destination?.let { destination ->
//                        onContinueClick(destination)
//                    }
//                },
//                initialPage = personasState.initialPage,
//                onPersonaSelected = viewModel::onPersonaSelect,
//                imageUrl = "",
//                personas = personas
//            )
//        } ?: run {
//            FullscreenCircularProgressContent()
//        }
//    }
// }
//
// @Composable
// fun ChooseDAppPersonaContent(
//    onBackClick: () -> Unit,
//    onContinueClick: () -> Unit,
//    initialPage: Boolean,
//    onPersonaSelected: (personaEntity: PersonaEntityUiState) -> Unit,
//    imageUrl: String,
//    personas: List<PersonaEntityUiState>,
//    modifier: Modifier = Modifier
// ) {
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//    ) {
//        IconButton(onClick = onBackClick) {
//            Icon(
//                imageVector = if (initialPage) Icons.Filled.Clear else Icons.Filled.ArrowBack,
//                contentDescription =  if (initialPage) "clear" else "navigate back"
//            )
//        }
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(horizontal = 50.dp, vertical = 16.dp)
//                .verticalScroll(rememberScrollState()),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(16.dp))
//            Image(
//                painter = rememberAsyncImagePainter(
//                    model = imageUrl,
//                    placeholder = painterResource(id = R.drawable.img_placeholder),
//                    error = painterResource(id = R.drawable.img_placeholder)
//                ),
//                contentDescription = "choose_dapp_login_image",
//                contentScale = ContentScale.Crop,
//                modifier = Modifier
//                    .size(110.dp)
//                    .clip(RoundedCornerShape(8.dp))
//            )
//            Spacer(modifier = Modifier.height(40.dp))
//            Text(
//                text = stringResource(id = R.string.choose_dapp_login_title),
//                textAlign = TextAlign.Center,
//                fontSize = 18.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                color = RadixGrey2,
//                text = stringResource(id = R.string.choose_dapp_login_body),
//                textAlign = TextAlign.Center,
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Normal
//            )
//            Spacer(modifier = Modifier.height(40.dp))
//
//            personas.forEach { personaEntityUiState ->
//                DAppPersonaCard(
//                    accountName = personaEntityUiState.personaEntity.accountName,
//                    name = personaEntityUiState.personaEntity.name,
//                    emailAddress = personaEntityUiState.personaEntity.emailAddress,
//                    selected = personaEntityUiState.selected,
//                    onSelectedChange = { selected ->
//                        onPersonaSelected(personaEntityUiState)
//                    }
//                )
//                Spacer(modifier = Modifier.height(16.dp))
//            }
//
//            Spacer(modifier = Modifier.height(30.dp))
//
//            TextButton(
//                colors = ButtonDefaults.buttonColors(
//                    backgroundColor = Color.Transparent,
//                    contentColor = MaterialTheme.colors.onBackground
//                ),
//                onClick = { /* TODO */ }
//            ) {
//                Text(
//                    text = stringResource(id = R.string.create_dapp_login_button_title),
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Normal
//                )
//            }
//
//            Spacer(Modifier.weight(1f))
//
//            Button(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 0.dp, vertical = 30.dp),
//                onClick = { onContinueClick() },
//                enabled = personas.any { it.selected },
//                colors = ButtonDefaults.buttonColors(
//                    backgroundColor = RadixButtonBackground,
//                    disabledBackgroundColor = RadixBackground
//                )
//            ) {
//                Text(
//                    color = White,
//                    text = stringResource(id = R.string.continue_button_title),
//                    modifier = Modifier.padding(26.dp, 8.dp, 26.dp, 8.dp)
//                )
//            }
//        }
//    }
// }
//
// @Preview(showBackground = true)
// @Preview("large font", fontScale = 2f, showBackground = true)
// @Composable
// fun ChooseDAppPersonaScreenPreview() {
// //    ChooseDAppPersonaScreen(
// //        onBackClick = {},
// //        onContinueClick = {},
// //        dismiss = false,
// //        onPersonaSelected = { _, _ -> },
// //        imageUrl = "",
// //        personas = listOf(
// //            PersonaEntityUiState(
// //                personaEntity = PersonaEntity(
// //                    accountName = "Account Name",
// //                    name = "Name",
// //                    emailAddress = "email@gmail.com",
// //                ),
// //                selected = false
// //            ),
// //            PersonaEntityUiState(
// //                personaEntity = PersonaEntity(
// //                    accountName = "Account Name",
// //                    name = "Name",
// //                    emailAddress = "email@gmail.com",
// //                ),
// //                selected = false
// //            )
// //        )
// //    )
// }
