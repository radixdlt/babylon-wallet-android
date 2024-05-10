package rdx.works.core.sargon

import com.radixdlt.sargon.AssetException
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedPersonaSimple
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.ProfileNetwork
import com.radixdlt.sargon.extensions.AssetsExceptionList
import com.radixdlt.sargon.extensions.AuthorizedDapps
import com.radixdlt.sargon.extensions.EntityFlags
import com.radixdlt.sargon.extensions.FactorSources
import com.radixdlt.sargon.extensions.Gateways
import com.radixdlt.sargon.extensions.P2pLinks
import com.radixdlt.sargon.extensions.Personas
import com.radixdlt.sargon.extensions.ProfileNetworks
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas

fun List<AssetException>.asIdentifiable() = AssetsExceptionList(assetExceptions = this)

fun List<AuthorizedPersonaSimple>.asIdentifiable() = ReferencesToAuthorizedPersonas(authorizedPersonasSimple = this)

fun List<Persona>.asIdentifiable() = Personas(personas = this)

fun List<ProfileNetwork>.asIdentifiable() = ProfileNetworks(networks = this)

fun List<FactorSource>.asIdentifiable() = FactorSources(factorSources = this)

fun List<EntityFlag>.asIdentifiable() = EntityFlags(entityFlags = this)

fun List<P2pLink>.asIdentifiable() = P2pLinks(p2pLinks = this)

fun List<Gateway>.asIdentifiable() = Gateways(gateways = this)

fun List<AuthorizedDapp>.asIdentifiable() = AuthorizedDapps(authorizedDapps = this)
