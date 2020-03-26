import { AppStateUpdater } from "../types/app";
import {
    FOTerm,
    LogicNode,
    LogicRelation,
    NCTableauxNode,
    NCTableauxState,
    NCTabTreeNode,
} from "../types/nc-tableaux";
import { VarAssign } from "../types/tableaux";
import { Tree } from "../types/tree";
import { sendMove } from "./api";
import { tree, treeLayout } from "./layout/tree";
import { estimateSVGTextWidth } from "./text-width";

export const sendAlpha = (
    server: string,
    state: NCTableauxState,
    stateChanger: AppStateUpdater,
    onError: (msg: string) => void,
    nodeID: number,
) =>
    sendMove(
        server,
        "nc-tableaux",
        state,
        { type: "alpha", nodeID },
        stateChanger,
        onError,
    );

export const sendBeta = (
    server: string,
    state: NCTableauxState,
    stateChanger: AppStateUpdater,
    onError: (msg: string) => void,
    nodeID: number,
) =>
    sendMove(
        server,
        "nc-tableaux",
        state,
        { type: "beta", nodeID },
        stateChanger,
        onError,
    );

export const sendGamma = (
    server: string,
    state: NCTableauxState,
    stateChanger: AppStateUpdater,
    onError: (msg: string) => void,
    nodeID: number,
) =>
    sendMove(
        server,
        "nc-tableaux",
        state,
        { type: "gamma", nodeID },
        stateChanger,
        onError,
    );

export const sendDelta = (
    server: string,
    state: NCTableauxState,
    stateChanger: AppStateUpdater,
    onError: (msg: string) => void,
    nodeID: number,
) =>
    sendMove(
        server,
        "nc-tableaux",
        state,
        { type: "delta", nodeID },
        stateChanger,
        onError,
    );

export const sendClose = (
    server: string,
    state: NCTableauxState,
    stateChanger: AppStateUpdater,
    onError: (msg: string) => void,
    nodeID: number,
    closeID: number,
    varAssign: VarAssign | null,
) =>
    sendMove(
        server,
        "nc-tableaux",
        state,
        { type: "close", nodeID, closeID, varAssign },
        stateChanger,
        onError,
    );

export const sendUndo = (
    server: string,
    state: NCTableauxState,
    stateChanger: AppStateUpdater,
    onError: (msg: string) => void,
) =>
    sendMove(
        server,
        "nc-tableaux",
        state,
        { type: "undo" },
        stateChanger,
        onError,
    );

export const ncTabTreeLayout = (nodes: NCTableauxNode[]) =>
    treeLayout(nodes, ncTabNodeToTree);

const ncTabNodeToTree = (
    nodes: NCTableauxNode[],
    n: NCTableauxNode = nodes[0],
    i: number = 0,
    y: number = 16,
): Tree<NCTabTreeNode> => {
    const width = estimateSVGTextWidth(n.spelling) + 56;
    return tree(
        width,
        72,
        y,
        { ...n, id: i },
        n.children.map((c) => ncTabNodeToTree(nodes, nodes[c], c, y + 72)),
    );
};

export const collectVarsFromTerm = (vars: string[], term: FOTerm) => {
    switch (term.type) {
        case "Constant":
            break;
        case "QuantifiedVariable":
            vars.push(term.spelling);
            break;
        case "Function":
            for (const arg of term.arguments) {
                collectVarsFromTerm(vars, arg);
            }
            break;
    }
};

export const collectVarsFromNode = (vars: string[], formula: LogicNode) => {
    let node: LogicRelation;
    if (formula.type === "not" && formula.child.type === "relation") {
        node = formula.child;
    } else if (formula.type === "relation") {
        node = formula;
    } else {
        return;
    }

    for (const arg of node.arguments) {
        collectVarsFromTerm(vars, arg);
    }
    return vars;
};
