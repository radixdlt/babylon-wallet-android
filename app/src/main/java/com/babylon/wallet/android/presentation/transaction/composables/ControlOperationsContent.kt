package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.title
import com.babylon.wallet.android.presentation.transaction.model.ControlOperation
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.PromptLabel
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.AccessRequirementForDisplay
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.BadgeForDisplay
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.RecoveryProposer
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.TimePeriodUnit
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.isNonFungible

/**
 * The access controller operations of a transaction or pre-authorization on accounts the user shares, e.g. a DAO's
 * account: per operation a heading, a card with the account and a row per effect, then, if it sets or stops rules, a
 * section of their own with the primary, recovery and confirmation roles.
 */
@Composable
fun ControlOperationsContent(
    operations: List<ControlOperation>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)
    ) {
        operations.forEach { operation ->
            ControlOperationView(operation = operation)
        }
    }
}

@Composable
private fun ControlOperationView(operation: ControlOperation) {
    Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)) {
        Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)) {
            SectionTitle(
                title = when (operation.heading) {
                    ControlOperation.Heading.UpdateAccountControl -> stringResource(R.string.controlOperation_heading_updateAccountControl)
                    ControlOperation.Heading.RecoveryFees -> stringResource(R.string.controlOperation_heading_recoveryFees)
                    ControlOperation.Heading.CreateAccount -> stringResource(R.string.controlOperation_heading_createAccount)
                    ControlOperation.Heading.CreatePersona -> stringResource(R.string.controlOperation_heading_createPersona)
                },
                iconRes = DSR.ic_entity_update_shield
            )

            OperationCard(operation = operation)
        }

        operation.rules?.let { rules ->
            Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)) {
                SectionTitle(
                    title = when (operation.rulesHeading) {
                        ControlOperation.RulesHeading.New -> stringResource(R.string.controlOperation_rules_new)
                        ControlOperation.RulesHeading.Pending -> stringResource(R.string.controlOperation_rules_pending)
                        ControlOperation.RulesHeading.Cancelled -> stringResource(R.string.controlOperation_rules_cancelled)
                        ControlOperation.RulesHeading.Rules -> stringResource(R.string.controlOperation_rules_rules)
                    },
                    iconRes = DSR.ic_security_key
                )

                Column(
                    modifier = Modifier
                        .card()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)
                ) {
                    RoleView(stringResource(R.string.controlOperation_role_primary), rules.primaryRole, operation.keyNames)
                    RoleView(stringResource(R.string.controlOperation_role_recovery), rules.recoveryRole, operation.keyNames)
                    RoleView(stringResource(R.string.controlOperation_role_confirmation), rules.confirmationRole, operation.keyNames)
                }
            }
        }
    }
}

/** The account the access controller controls, then what the operation does. */
@Composable
private fun OperationCard(operation: ControlOperation) {
    Column(modifier = Modifier.card()) {
        val hasEffects = operation.effects.isNotEmpty()
        // The preview's address isn't the one a created entity will have, so none is shown.
        if (operation.createsEntity) {
            NewEntityHeader(
                isAccount = operation.controlledEntity is AddressOfAccountOrPersona.Account,
                hasEffects = hasEffects
            )
        } else when (val entity = operation.controlledEntity) {
            is AddressOfAccountOrPersona.Account -> AccountCardHeader(
                // An account in the Profile, e.g. a shared one, is drawn as the user's.
                account = operation.ownedAccount?.let { InvolvedAccount.Owned(it) }
                    ?: InvolvedAccount.Other(address = entity.v1, addressBookName = operation.accountName),
                shape = if (hasEffects) RadixTheme.shapes.roundedRectTopMedium else RadixTheme.shapes.roundedRectMedium
            )

            is AddressOfAccountOrPersona.Identity -> ActionableAddressView(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                address = Address.Identity(entity.v1),
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = RadixTheme.colors.textSecondary
            )

            null -> operation.accessController?.let { accessController ->
                ActionableAddressView(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    address = Address.AccessController(accessController),
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = RadixTheme.colors.textSecondary
                )
            }
        }

        if (hasEffects) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.backgroundSecondary, RadixTheme.shapes.roundedRectBottomMedium)
                    .padding(RadixTheme.dimensions.paddingDefault),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                operation.effects.forEach { effect -> EffectRow(effect) }
            }
        }
    }
}

/** An account or persona the manifest creates, drawn like the header for accounts not in this wallet, but without an address. */
@Composable
private fun NewEntityHeader(isAccount: Boolean, hasEffects: Boolean) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (RadixTheme.config.isDarkTheme) RadixTheme.colors.backgroundTertiary else RadixTheme.colors.iconSecondary,
                shape = if (hasEffects) RadixTheme.shapes.roundedRectTopMedium else RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        text = stringResource(if (isAccount) R.string.controlOperation_newAccount else R.string.controlOperation_newPersona),
        style = RadixTheme.typography.body1Header,
        color = Color.White,
        maxLines = 1
    )
}

/** An effect: an icon in the text color and a bold line. */
@Composable
private fun EffectRow(effect: ControlOperation.Effect) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = effect.icon,
            contentDescription = null,
            tint = RadixTheme.colors.text
        )
        Text(
            text = effect.text(),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )
    }
}

private val ControlOperation.Effect.icon: ImageVector
    get() = when (this) {
        ControlOperation.Effect.Applied,
        ControlOperation.Effect.ConfirmsPendingUpdate,
        ControlOperation.Effect.CompletesTimedUpdate -> Icons.Outlined.CheckCircle
        is ControlOperation.Effect.Timed,
        is ControlOperation.Effect.CurrentTimedRecoveryDelay,
        is ControlOperation.Effect.TimedRecoveryDelay -> Icons.Outlined.Schedule
        is ControlOperation.Effect.ProposedBy -> Icons.Outlined.Person
        is ControlOperation.Effect.StopsTimer -> Icons.Outlined.TimerOff
        is ControlOperation.Effect.CancelsPendingUpdate -> Icons.Outlined.Block
        ControlOperation.Effect.Created,
        is ControlOperation.Effect.AddsToRecoveryFee -> Icons.Outlined.AddCircleOutline
        is ControlOperation.Effect.WithdrawsFromRecoveryFee -> Icons.Outlined.RemoveCircleOutline
        ControlOperation.Effect.LocksPrimaryRole -> LockedPadlock
        ControlOperation.Effect.UnlocksPrimaryRole -> UnlockedPadlock
    }

// Material's open lock barely differs from its closed one at this size, so both are drawn here, the open one with its
// shackle lifted clear of the body, as iOS's lock.open has it.
private val LockedPadlock: ImageVector = padlock(isOpen = false)
private val UnlockedPadlock: ImageVector = padlock(isOpen = true)

private fun padlock(isOpen: Boolean): ImageVector = ImageVector.Builder(
    name = if (isOpen) "UnlockedPadlock" else "LockedPadlock",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    val stroke = SolidColor(Color.Black)
    // The body
    path(stroke = stroke, strokeLineWidth = 2f, strokeLineJoin = StrokeJoin.Round) {
        moveTo(7f, 11f)
        lineTo(17f, 11f)
        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 13f)
        lineTo(19f, 19f)
        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17f, 21f)
        lineTo(7f, 21f)
        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 19f)
        lineTo(5f, 13f)
        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 11f)
        close()
    }
    // The shackle, its right side down into the body when locked, lifted out of it when open
    path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(8f, 11f)
        lineTo(8f, 7f)
        arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 7f)
        if (!isOpen) lineTo(16f, 11f)
    }
    // The keyhole
    path(fill = stroke) {
        moveTo(13.25f, 16f)
        arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.75f, 16f)
        arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 13.25f, 16f)
        close()
    }
}.build()

@Composable
private fun ControlOperation.Effect.text(): String = when (this) {
    ControlOperation.Effect.Applied -> stringResource(R.string.controlOperation_effect_applied)
    is ControlOperation.Effect.Timed -> delayInMinutes?.let {
        stringResource(R.string.controlOperation_effect_timed, periodTitle(minutes = it))
    } ?: stringResource(R.string.controlOperation_effect_timedNoDelay)
    is ControlOperation.Effect.CurrentTimedRecoveryDelay -> delayInMinutes?.let {
        stringResource(R.string.controlOperation_effect_currentDelay, periodTitle(minutes = it))
    } ?: stringResource(R.string.controlOperation_effect_noCurrentDelay)
    is ControlOperation.Effect.ProposedBy -> when (proposer) {
        RecoveryProposer.PRIMARY -> stringResource(R.string.controlOperation_effect_proposedByPrimary)
        RecoveryProposer.RECOVERY -> stringResource(R.string.controlOperation_effect_proposedByRecovery)
    }
    ControlOperation.Effect.LocksPrimaryRole -> stringResource(R.string.controlOperation_effect_locksPrimary)
    ControlOperation.Effect.UnlocksPrimaryRole -> stringResource(R.string.controlOperation_effect_unlocksPrimary)
    ControlOperation.Effect.ConfirmsPendingUpdate -> stringResource(R.string.controlOperation_effect_confirms)
    ControlOperation.Effect.CompletesTimedUpdate -> stringResource(R.string.controlOperation_effect_completesTimed)
    is ControlOperation.Effect.StopsTimer -> if (canStillBeConfirmed) {
        stringResource(R.string.controlOperation_effect_stopsTimer)
    } else {
        stringResource(R.string.controlOperation_effect_stopsTimerOnly)
    }
    is ControlOperation.Effect.CancelsPendingUpdate -> when (proposer) {
        RecoveryProposer.PRIMARY -> stringResource(R.string.controlOperation_effect_cancelsPrimary)
        RecoveryProposer.RECOVERY -> stringResource(R.string.controlOperation_effect_cancelsRecovery)
    }
    ControlOperation.Effect.Created -> stringResource(R.string.controlOperation_effect_created)
    is ControlOperation.Effect.TimedRecoveryDelay ->
        stringResource(R.string.controlOperation_effect_timedRecoveryDelay, periodTitle(minutes = delayInMinutes))
    is ControlOperation.Effect.AddsToRecoveryFee ->
        stringResource(R.string.controlOperation_effect_addsToRecoveryFee, xrdAmount.formatted())
    is ControlOperation.Effect.WithdrawsFromRecoveryFee ->
        stringResource(R.string.controlOperation_effect_withdrawsFromRecoveryFee, xrdAmount.formatted())
}

/**
 * The period as the shield setup names it ("3 days", "2 weeks"), or in minutes when it isn't a whole number of days.
 * A period too long for those, whose value doesn't fit their [TimePeriod], is spelled out in days, hours and minutes
 * instead, so it is never cut short.
 */
@Composable
private fun periodTitle(minutes: UInt): String {
    val minutesPerDay = 24u * 60u
    val minutesPerWeek = 7u * minutesPerDay
    val (value, unit) = when {
        minutes != 0u && minutes % minutesPerWeek == 0u -> minutes / minutesPerWeek to TimePeriodUnit.WEEKS
        minutes != 0u && minutes % minutesPerDay == 0u -> minutes / minutesPerDay to TimePeriodUnit.DAYS
        else -> minutes to TimePeriodUnit.MINUTES
    }
    return if (value <= UShort.MAX_VALUE.toUInt()) {
        TimePeriod(value = value.toUShort(), unit = unit).title()
    } else {
        daysHoursMinutes(minutes)
    }
}

/** e.g. "52 days, 3 hours, 1 minute", leaving out the parts that are zero. */
@Composable
private fun daysHoursMinutes(minutes: UInt): String {
    val total = minutes.toLong()
    val days = total / (24 * 60)
    val hours = total % (24 * 60) / 60
    val remainingMinutes = total % 60
    return listOfNotNull(
        days.takeIf { it > 0 }?.let {
            if (it == 1L) {
                stringResource(R.string.controlOperation_period_day)
            } else {
                stringResource(R.string.controlOperation_period_days, it)
            }
        },
        hours.takeIf { it > 0 }?.let {
            if (it == 1L) {
                stringResource(R.string.controlOperation_period_hour)
            } else {
                stringResource(R.string.controlOperation_period_hours, it)
            }
        },
        remainingMinutes.takeIf { it > 0 }?.let {
            if (it == 1L) {
                stringResource(R.string.controlOperation_period_minute)
            } else {
                stringResource(R.string.controlOperation_period_minutes, it)
            }
        }
    ).joinToString(separator = ", ")
}

@Composable
private fun RoleView(title: String, requirement: AccessRequirementForDisplay, keyNames: Map<NonFungibleGlobalId, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
        Text(text = title, style = RadixTheme.typography.body1Header, color = RadixTheme.colors.text)
        RequirementView(requirement = requirement, keyNames = keyNames)
    }
}

/** A whole requirement, at the top of a role or of an expanded summary. */
@Composable
private fun RequirementView(requirement: AccessRequirementForDisplay, keyNames: Map<NonFungibleGlobalId, String>) {
    when (requirement) {
        AccessRequirementForDisplay.Anyone -> StateCard(
            title = stringResource(R.string.controlOperation_requirement_anyone),
            subtitle = stringResource(R.string.controlOperation_requirement_anyoneSubtitle),
            color = RadixTheme.colors.error,
            icon = painterResource(id = DSR.ic_warning_error)
        )

        AccessRequirementForDisplay.NoOne -> StateCard(
            title = stringResource(R.string.controlOperation_requirement_noOne),
            subtitle = stringResource(R.string.controlOperation_requirement_noOneSubtitle),
            color = RadixTheme.colors.textSecondary,
            icon = rememberVectorPainter(Icons.Outlined.Lock)
        )

        // A list is a group, outlined like a way, wherever it is.
        is AccessRequirementForDisplay.AnyOf -> if (requirement.requirements.all { it.isLeaf }) {
            WayCard { LeafList(stringResource(R.string.controlOperation_requirement_anyOfThese), requirement.requirements, keyNames) }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
                requirement.requirements.forEachIndexed { index, way ->
                    if (index > 0) Connector(stringResource(R.string.controlOperation_requirement_or))
                    if (way.drawsOwnCard(asPart = false)) {
                        WayView(way, keyNames)
                    } else {
                        WayCard { WayView(way, keyNames) }
                    }
                }
            }
        }

        is AccessRequirementForDisplay.AllOf -> if (requirement.requirements.all { it.isLeaf }) {
            WayCard { LeafList(stringResource(R.string.controlOperation_requirement_allOfThese), requirement.requirements, keyNames) }
        } else {
            WayCard { WayView(requirement, keyNames) }
        }

        is AccessRequirementForDisplay.CountOf -> WayCard {
            LeafList(
                header = stringResource(R.string.controlOperation_requirement_countOf, requirement.count.toInt(), requirement.badges.size),
                leaves = requirement.badges.map { AccessRequirementForDisplay.Badge(it) },
                keyNames = keyNames
            )
        }

        is AccessRequirementForDisplay.Badge,
        is AccessRequirementForDisplay.AmountOf -> LeafView(requirement, keyNames)
    }
}

/** One way of passing a role, drawn inside a card. */
@Composable
private fun WayView(requirement: AccessRequirementForDisplay, keyNames: Map<NonFungibleGlobalId, String>) {
    if (requirement is AccessRequirementForDisplay.AllOf) {
        val parts = requirement.requirements
        if (parts.all { it.isLeaf }) {
            LeafList(stringResource(R.string.controlOperation_requirement_allOfThese), parts, keyNames)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
                parts.forEachIndexed { index, part ->
                    // Joins parts as the OR pill joins ways.
                    if (index > 0) Connector(stringResource(R.string.controlOperation_requirement_and))
                    if (part.drawsOwnCard(asPart = true)) {
                        PartView(part, keyNames)
                    } else {
                        // A list joined by AND is boxed, so it reads as one part.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, RadixTheme.colors.border, RadixTheme.shapes.roundedRectSmall)
                                .padding(RadixTheme.dimensions.paddingMedium)
                        ) {
                            PartView(part, keyNames)
                        }
                    }
                }
            }
        }
    } else {
        PartView(requirement, keyNames)
    }
}

/** A part of a way: a badge, a list of badges, or a summary of anything deeper. */
@Composable
private fun PartView(requirement: AccessRequirementForDisplay, keyNames: Map<NonFungibleGlobalId, String>) {
    when {
        requirement is AccessRequirementForDisplay.AnyOf && requirement.requirements.all { it.isLeaf } ->
            LeafList(stringResource(R.string.controlOperation_requirement_anyOfThese), requirement.requirements, keyNames)

        requirement is AccessRequirementForDisplay.CountOf -> LeafList(
            header = stringResource(R.string.controlOperation_requirement_countOf, requirement.count.toInt(), requirement.badges.size),
            leaves = requirement.badges.map { AccessRequirementForDisplay.Badge(it) },
            keyNames = keyNames
        )

        requirement is AccessRequirementForDisplay.AllOf && requirement.requirements.all { it.isLeaf } ->
            LeafList(stringResource(R.string.controlOperation_requirement_allOfThese), requirement.requirements, keyNames)

        requirement.isLeaf -> LeafView(requirement, keyNames)

        else -> SummaryRow(requirement, keyNames)
    }
}

@Composable
private fun LeafList(header: String, leaves: List<AccessRequirementForDisplay>, keyNames: Map<NonFungibleGlobalId, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
        Text(
            modifier = Modifier
                .background(RadixTheme.colors.backgroundTertiary, RadixTheme.shapes.roundedRectXSmall)
                .padding(horizontal = RadixTheme.dimensions.paddingSmall, vertical = RadixTheme.dimensions.paddingXXXSmall),
            text = header,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.text
        )
        leaves.forEach { leaf -> LeafView(leaf, keyNames) }
    }
}

@Composable
private fun LeafView(requirement: AccessRequirementForDisplay, keyNames: Map<NonFungibleGlobalId, String>) {
    when (requirement) {
        is AccessRequirementForDisplay.Badge -> BadgeView(requirement.badge, keyNames)
        is AccessRequirementForDisplay.AmountOf -> ResourceBadgeRow(resource = requirement.resource, amount = requirement.amount)
        else -> SummaryRow(requirement, keyNames)
    }
}

@Composable
private fun BadgeView(badge: BadgeForDisplay, keyNames: Map<NonFungibleGlobalId, String>) {
    when (badge) {
        // As the factor sources are drawn on the shield screens.
        is BadgeForDisplay.OwnFactor -> FactorSourceCardView(
            item = badge.factorSource.toFactorSourceCard(includeLastUsedOn = false),
            castsShadow = false,
            isOutlined = true
        )

        // Drawn like a signature resource on the MFA signature resources screen, with a dashed outline as it isn't in
        // this wallet, and named as in the address book. A key without a name stands out as one the user hasn't met.
        is BadgeForDisplay.UnknownKey -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.cardSecondary, RadixTheme.shapes.roundedRectSmall)
                .dashedBorder(RadixTheme.colors.textSecondary)
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = DSR.ic_nfts),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
            ) {
                val name = keyNames[badge.badge]
                Text(
                    text = name ?: stringResource(R.string.controlOperation_badge_unknownKey),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text,
                    maxLines = 1
                )
                // Cut off in the middle, so that the end of the local id, which tells keys apart, stays readable.
                ActionableAddressView(
                    globalId = badge.badge,
                    showOnlyLocalId = false,
                    textStyle = RadixTheme.typography.body2Regular,
                    textColor = RadixTheme.colors.textSecondary,
                    iconColor = RadixTheme.colors.iconSecondary,
                    isSingleLine = true
                )
                if (name == null) {
                    PromptLabel(
                        text = stringResource(R.string.controlOperation_badge_notInAddressBook),
                        textStyle = RadixTheme.typography.body2HighImportance,
                        iconSize = 16.dp
                    )
                }
            }
        }

        is BadgeForDisplay.NonFungible -> ResourceBadgeRow(resource = badge.badge.resourceAddress, nonFungible = badge.badge)
        is BadgeForDisplay.Resource -> ResourceBadgeRow(resource = badge.resource)
    }
}

/** A badge that is a resource, an amount of one or an NFT. */
@Composable
private fun ResourceBadgeRow(
    resource: ResourceAddress,
    amount: Decimal192? = null,
    nonFungible: NonFungibleGlobalId? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RadixTheme.colors.border, RadixTheme.shapes.roundedRectSmall)
            .padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = if (resource.isNonFungible) DSR.ic_nfts else DSR.ic_token),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)) {
            if (nonFungible != null) {
                ActionableAddressView(
                    globalId = nonFungible,
                    showOnlyLocalId = false,
                    textStyle = RadixTheme.typography.body1Header,
                    textColor = RadixTheme.colors.text,
                    isSingleLine = true
                )
            } else {
                ActionableAddressView(
                    address = Address.Resource(resource),
                    textStyle = RadixTheme.typography.body1Header,
                    textColor = RadixTheme.colors.text
                )
                Text(
                    text = when {
                        amount != null -> amount.formatted()
                        resource.isNonFungible -> stringResource(R.string.controlOperation_requirement_anyNft)
                        // Any amount of a token passes, so say so where the amount would be.
                        else -> stringResource(R.string.controlOperation_requirement_anyAmount)
                    },
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.textSecondary
                )
            }
        }
    }
}

/** Stands in for a requirement too deep to draw in place, and opens it in place when tapped. */
@Composable
private fun SummaryRow(requirement: AccessRequirementForDisplay, keyNames: Map<NonFungibleGlobalId, String>) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    // A group's subtitle says nothing about what's in it, as the group may hold anything: it is only opened to review.
    val (title, subtitle) = when (requirement) {
        is AccessRequirementForDisplay.AnyOf ->
            stringResource(R.string.controlOperation_requirement_oneOfOptions, requirement.requirements.size) to
                stringResource(R.string.controlOperation_requirement_tapToReview)
        is AccessRequirementForDisplay.AllOf ->
            stringResource(R.string.controlOperation_requirement_allOfParts, requirement.requirements.size) to
                stringResource(R.string.controlOperation_requirement_tapToReview)
        AccessRequirementForDisplay.Anyone -> stringResource(R.string.controlOperation_requirement_anyone) to
            stringResource(R.string.controlOperation_requirement_anyoneSubtitle)
        AccessRequirementForDisplay.NoOne -> stringResource(R.string.controlOperation_requirement_noOne) to
            stringResource(R.string.controlOperation_requirement_noOneSubtitle)
        else -> "" to ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RadixTheme.shapes.roundedRectSmall)
                .background(RadixTheme.colors.backgroundSecondary)
                .throttleClickable { isExpanded = !isExpanded }
                .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = RadixTheme.typography.body1Header, color = RadixTheme.colors.text)
                Text(text = subtitle, style = RadixTheme.typography.body2Regular, color = RadixTheme.colors.textSecondary)
            }
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = RadixTheme.colors.iconSecondary
            )
        }
        if (isExpanded) {
            RequirementView(requirement, keyNames)
        }
    }
}

@Composable
private fun WayCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.background, RadixTheme.shapes.roundedRectSmall)
            .border(1.dp, RadixTheme.colors.border, RadixTheme.shapes.roundedRectSmall)
            .padding(RadixTheme.dimensions.paddingMedium)
    ) {
        content()
    }
}

@Composable
private fun Connector(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = RadixTheme.colors.border)
        Text(
            modifier = Modifier
                .border(1.dp, RadixTheme.colors.border, CircleShape)
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            text = text,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.textSecondary
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = RadixTheme.colors.border)
    }
}

@Composable
private fun StateCard(title: String, subtitle: String, color: Color, icon: Painter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color, RadixTheme.shapes.roundedRectSmall)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(modifier = Modifier.size(20.dp), painter = icon, contentDescription = null, tint = color)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = RadixTheme.typography.body1Header, color = color)
            Text(text = subtitle, style = RadixTheme.typography.body2Regular, color = RadixTheme.colors.textSecondary)
        }
    }
}

/** The review's card, as for a shield update. */
@Composable
private fun Modifier.card() = this
    .fillMaxWidth()
    .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
    .background(RadixTheme.colors.background, RadixTheme.shapes.roundedRectDefault)
    .clip(RadixTheme.shapes.roundedRectDefault)

/** A dashed outline, for a key that isn't in this wallet. */
private fun Modifier.dashedBorder(color: Color) = drawBehind {
    val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
    drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(8.dp.toPx()))
}

private val AccessRequirementForDisplay.isLeaf: Boolean
    get() = this is AccessRequirementForDisplay.Badge || this is AccessRequirementForDisplay.AmountOf

/**
 * Whether, as a way or as a part joined by AND, it is drawn as a card of its own (a badge or a summary row) and so
 * needs no box around it. A way that joins groups by AND is drawn in place, while a part is summarized.
 */
private fun AccessRequirementForDisplay.drawsOwnCard(asPart: Boolean): Boolean = when (this) {
    is AccessRequirementForDisplay.Badge,
    is AccessRequirementForDisplay.AmountOf,
    AccessRequirementForDisplay.Anyone,
    AccessRequirementForDisplay.NoOne -> true
    is AccessRequirementForDisplay.AnyOf -> !requirements.all { it.isLeaf }
    is AccessRequirementForDisplay.AllOf -> asPart && !requirements.all { it.isLeaf }
    is AccessRequirementForDisplay.CountOf -> false
}
