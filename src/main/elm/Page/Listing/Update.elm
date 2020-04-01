module Page.Listing.Update exposing (update)

import Page.Listing.Data exposing (..)


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        AllScripts (Ok scripts) ->
            ( { model | scripts = scripts }
            , Cmd.none
            )

        AllScripts (Err _) ->
            ( model, Cmd.none )

        SetCategory c ->
            ( { model | category = c }
            , Cmd.none
            )
