package kalkulierbar.resolution

import kalkulierbar.CloseMessage
import kalkulierbar.IllegalMove
import kalkulierbar.JSONCalculus
import kalkulierbar.JsonParseException
import kalkulierbar.clause.Atom
import kalkulierbar.clause.Clause
import kalkulierbar.clause.ClauseSet
import kalkulierbar.parsers.CnfStrategy
import kalkulierbar.parsers.FlexibleClauseSetParser
import kalkulierbar.tamperprotect.ProtectedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

class PropositionalResolution : JSONCalculus<ResolutionState, ResolutionMove, ResolutionParam>() {
    override val identifier = "prop-resolution"

    override fun parseFormulaToState(formula: String, params: ResolutionParam?): ResolutionState {
        val parsed = if (params == null)
            FlexibleClauseSetParser.parse(formula)
        else
            FlexibleClauseSetParser.parse(formula, params.cnfStrategy)

        return ResolutionState(parsed, params?.highlightSelectable ?: false)
    }

    override fun applyMoveOnState(state: ResolutionState, move: ResolutionMove): ResolutionState {
        val cId1 = move.c1
        val cId2 = move.c2
        val clauses = state.clauseSet.clauses
        var spelling = move.spelling

        // Verify that the clause ids are valid
        if (cId1 == cId2)
            throw IllegalMove("Both ids refer to the same clause")
        if (cId1 < 0 || cId1 >= clauses.size)
            throw IllegalMove("There is no clause with id $cId1")
        if (cId2 < 0 || cId2 >= clauses.size)
            throw IllegalMove("There is no clause with id $cId2")

        val c1 = clauses[cId1]
        val c2 = clauses[cId2]
        val resCandidates: Pair<Atom<String>, Atom<String>>

        // If the frontend did not pass a resolution target, we'll try to find one ourselves
        if (spelling == null) {
            resCandidates = getAutoResolutionCandidates(c1, c2)
        } else {
            // Filter clauses for atoms with correct spelling
            val atomsInC1 = c1.atoms.filter { it.lit == spelling }
            val atomsInC2 = c2.atoms.filter { it.lit == spelling }
            if (atomsInC1.isEmpty())
                throw IllegalMove("Clause '$c1' does not contain atoms with spelling '$spelling'")
            if (atomsInC2.isEmpty())
                throw IllegalMove("Clause '$c2' does not contain atoms with spelling '$spelling'")

            val msg = "Clauses '$c1' and '$c2' do not contain atom '$spelling' in both positive and negated form"
            resCandidates = findResCandidates(atomsInC1, atomsInC2)
                    ?: throw IllegalMove(msg)
        }

        val (a1, a2) = resCandidates

        // Add the new node where the second one was. This should be pretty nice for the user
        state.newestNode = cId2

        clauses.add(state.newestNode, buildClause(c1, a1, c2, a2))

        return state
    }

    /**
     * Automatically find a resolution candidate for two given clauses
     * @param c1 First clause to resolve
     * @param c2 Second clause to resolve
     * @return Pair of suitable atoms in c1 and c2 for resolution
     */
    private fun getAutoResolutionCandidates(c1: Clause<String>, c2: Clause<String>): Pair<Atom<String>, Atom<String>> {

        // Find variables present in both clauses
        var sharedAtoms = c1.atoms.filter {
            val c1atom = it.lit
            c2.atoms.any { it.lit == c1atom }
        }

        if (sharedAtoms.isEmpty())
            throw IllegalMove("Clauses '$c1' and '$c2' contain no common variables")

        // Sort out atoms not present in opposite polarity in c2 (shared atoms came from c1 originally)
        sharedAtoms = sharedAtoms.filter {
            c2.atoms.contains(it.not())
        }

        if (sharedAtoms.isEmpty())
            throw IllegalMove("Clauses '$c1' and '$c2' contain no common variables that appear" +
                "in positive and negated form")

        // Choose the first shared variable
        val a1 = sharedAtoms[0]
        val a2 = c2.atoms.filter { it == a1.not() }[0]

        return Pair(a1, a2)
    }

    /**
     * Searches two atom lists for resolution candidates and returns the first.
     * The lists have to be filtered for the spelling already.
     * @param atoms1 The first list of atoms
     * @param atoms2 The second list of atoms
     * @return A pair of the two atoms for resolution.
     */
    private fun findResCandidates(
        atoms1: List<Atom<String>>,
        atoms2: List<Atom<String>>
    ): Pair<Atom<String>, Atom<String>>? {
        val (pos, neg) = atoms2.partition { !it.negated }

        for (a1 in atoms1) {
            val other = if (a1.negated) pos else neg
            if (other.isEmpty())
                continue
            val a2 = other[0]
            return Pair(a1, a2)
        }

        return null
    }

    /**
     * Builds a new clause according to resolution.
     * @param c1 The first clause for resolution
     * @param a1 The atom to filter out of c1
     * @param c2 The second clause for resolution
     * @param a2 The atom to filter out of c2
     * @return A new clause that contains all elements of c1 and c2 except for a1 and a2
     */
    private fun buildClause(
        c1: Clause<String>,
        a1: Atom<String>,
        c2: Clause<String>,
        a2: Atom<String>
    ): Clause<String> {
        val atoms = c1.atoms.filter { it != a1 }.toMutableList() +
                c2.atoms.filter { it != a2 }.toMutableList()
        return Clause(atoms.distinct().toMutableList())
    }

    override fun checkCloseOnState(state: ResolutionState): CloseMessage {
        val hasEmptyClause = state.clauseSet.clauses.any { it.atoms.isEmpty() }
        val msg = if (hasEmptyClause) "The proof is closed" else "The proof is not closed"
        return CloseMessage(hasEmptyClause, msg)
    }

    @Suppress("TooGenericExceptionCaught")
    @UnstableDefault
    override fun jsonToState(json: String): ResolutionState {
        try {
            val parsed = Json.parse(ResolutionState.serializer(), json)

            // Ensure valid, unmodified state object
            if (!parsed.verifySeal())
                throw JsonParseException("Invalid tamper protection seal, state object appears to have been modified")

            return parsed
        } catch (e: Exception) {
            val msg = "Could not parse JSON state: "
            throw JsonParseException(msg + (e.message ?: "Unknown error"))
        }
    }

    @UnstableDefault
    override fun stateToJson(state: ResolutionState): String {
        state.computeSeal()
        return Json.stringify(ResolutionState.serializer(), state)
    }

    @Suppress("TooGenericExceptionCaught")
    @UnstableDefault
    override fun jsonToMove(json: String): ResolutionMove {
        try {
            return Json.parse(ResolutionMove.serializer(), json)
        } catch (e: Exception) {
            val msg = "Could not parse JSON move: "
            throw JsonParseException(msg + (e.message ?: "Unknown error"))
        }
    }

    /*
     * Parses a JSON parameter representation into a ResolutionParam object
     * @param json JSON parameter representation
     * @return parsed param object
     */
    @Suppress("TooGenericExceptionCaught")
    @UnstableDefault
    override fun jsonToParam(json: String): ResolutionParam {
        try {
            return Json.parse(ResolutionParam.serializer(), json)
        } catch (e: Exception) {
            val msg = "Could not parse JSON params: "
            throw JsonParseException(msg + (e.message ?: "Unknown error"))
        }
    }
}

@Serializable
class ResolutionState(val clauseSet: ClauseSet<String>, val highlightSelectable: Boolean) : ProtectedState() {
    var newestNode = -1

    override var seal = ""

    override fun getHash(): String {
        val clauseSetHash = clauseSet.toString()
        return "resolutionstate|$clauseSetHash|$highlightSelectable|$newestNode"
    }
}

@Serializable
data class ResolutionMove(val c1: Int, val c2: Int, val spelling: String?)

@Serializable
data class ResolutionParam(val cnfStrategy: CnfStrategy, val highlightSelectable: Boolean)
