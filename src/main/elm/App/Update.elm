module App.Update exposing (initCmd, update)

import Api
import App.Data exposing (..)
import Browser exposing (UrlRequest(..))
import Browser.Navigation as Nav
import Page exposing (Page(..))
import Page.Create.Data
import Page.Create.Update
import Page.Detail.Data
import Page.Detail.Update
import Page.Listing.Data
import Page.Listing.Update
import Url


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        AllScripts res ->
            updateListing (Page.Listing.Data.AllScripts res) model

        ListingMsg lm ->
            updateListing lm model

        DetailMsg dm ->
            updateDetail dm model

        CreateMsg cm ->
            updateCreate cm model

        SetPage p ->
            ( { model | page = p }
            , Cmd.none
            )

        VersionResp (Ok info) ->
            ( { model | version = info }, Cmd.none )

        VersionResp (Err err) ->
            ( model, Cmd.none )

        NavRequest req ->
            case req of
                Internal url ->
                    let
                        isCurrent =
                            Page.fromUrl url
                                |> Maybe.map (\p -> p == model.page)
                                |> Maybe.withDefault True
                    in
                    ( model
                    , if isCurrent then
                        Cmd.none

                      else
                        Nav.pushUrl model.key (Url.toString url)
                    )

                External url ->
                    ( model
                    , Nav.load url
                    )

        NavChange url ->
            let
                page =
                    Page.fromUrl url |> Maybe.withDefault ListingPage

                ( m, c ) =
                    initPage model page
            in
            ( { m | page = page }, c )


updateListing : Page.Listing.Data.Msg -> Model -> ( Model, Cmd Msg )
updateListing lmsg model =
    let
        ( lm, lc ) =
            Page.Listing.Update.update lmsg model.listingModel
    in
    ( { model | listingModel = lm }
    , Cmd.map ListingMsg lc
    )


updateDetail : Page.Detail.Data.Msg -> Model -> ( Model, Cmd Msg )
updateDetail dmsg model =
    let
        ( dm, dc ) =
            Page.Detail.Update.update dmsg model.detailModel
    in
    ( { model | detailModel = dm }
    , Cmd.map DetailMsg dc
    )


updateCreate : Page.Create.Data.Msg -> Model -> ( Model, Cmd Msg )
updateCreate cmsg model =
    let
        ( cm, cc ) =
            Page.Create.Update.update cmsg model.createModel
    in
    ( { model | createModel = cm }
    , Cmd.map CreateMsg cc
    )


initPage : Model -> Page -> ( Model, Cmd Msg )
initPage model page =
    case page of
        DetailPage name ->
            updateDetail (Page.Detail.Data.ChangeName name) model

        ListingPage ->
            ( model, Cmd.none )

        CreatePage ->
            ( model, Cmd.none )


initCmd : Model -> Cmd Msg
initCmd model =
    case model.page of
        ListingPage ->
            Api.fetchScripts model.flags.apiBase (Page.Listing.Data.AllScripts >> ListingMsg)

        DetailPage name ->
            Api.scriptDetail model.flags.apiBase name (Page.Detail.Data.ScriptDetailRes >> DetailMsg)

        CreatePage ->
            Cmd.none
