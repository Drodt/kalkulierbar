import {
    ResolutionMove,
    ResolutionParams,
    ResolutionState
} from "./resolution";
import { TableauxMove, TableauxParams, TableauxState } from "./tableaux";

export type Calculus = "prop-tableaux" | "prop-resolution";

export interface Move {
    "prop-tableaux": TableauxMove;
    "prop-resolution": ResolutionMove;
}

export interface Params {
    "prop-tableaux": TableauxParams;
    "prop-resolution": ResolutionParams;
}

export enum Theme {
    dark = "dark",
    light = "light",
    auto = "auto"
}

export interface AppState {
    server: string;
    notification?: Notification;
    smallScreen: boolean;
    theme: Theme;
    "prop-tableaux"?: TableauxState;
    "prop-resolution"?: ResolutionState;
}

export interface DerivedAppState extends AppState {
    onError: (msg: string) => void;
    onSuccess: (msg: string) => void;
    onMessage: (msg: string, type: NotificationType) => void;
    removeNotification: () => void;
    onChange: <C extends Calculus = Calculus>(
        calculus: C,
        state: AppState[C]
    ) => void;
    dispatch: (a: AppStateAction) => void;
}

interface AppStateActionBase {
    type: AppStateActionType;
}

export enum AppStateActionType {
    SET_SMALL_SCREEN,
    ADD_NOTIFICATION,
    REMOVE_NOTIFICATION,
    UPDATE_CALCULUS_STATE,
    SET_THEME
}

export interface SetSmallScreen extends AppStateActionBase {
    type: AppStateActionType.SET_SMALL_SCREEN;
    value: boolean;
}

export interface AddNotification extends AppStateActionBase {
    type: AppStateActionType.ADD_NOTIFICATION;
    value: Notification;
}

export interface RemoveNotification extends AppStateActionBase {
    type: AppStateActionType.REMOVE_NOTIFICATION;
}

export interface UpdateCalculusState<C extends Calculus = Calculus>
    extends AppStateActionBase {
    type: AppStateActionType.UPDATE_CALCULUS_STATE;
    calculus: C;
    value: AppState[C];
}

export interface SetTheme extends AppStateActionBase {
    type: AppStateActionType.SET_THEME;
    value: Theme;
}

export type AppStateAction =
    | SetSmallScreen
    | AddNotification
    | RemoveNotification
    | UpdateCalculusState
    | SetTheme;

export type AppStateUpdater = <C extends Calculus = Calculus>(
    id: C,
    newState: AppState[C]
) => void;

export enum NotificationType {
    Error,
    Success,
    None
}

export interface Notification {
    message: string;
    type: NotificationType;
}

export interface CheckCloseResponse {
    closed: boolean;
    msg: string;
}
