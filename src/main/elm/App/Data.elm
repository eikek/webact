module App.Data exposing (..)

import Browser exposing (UrlRequest)
import Browser.Navigation exposing (Key)
import Data.Flags exposing (Flags)
import Data.ScriptInfo exposing (ScriptInfo)
import Data.Version exposing (Version)
import Http
import Page exposing (Page(..))
import Page.Create.Data
import Page.Detail.Data
import Page.Listing.Data
import Url exposing (Url)


type alias Model =
    { flags : Flags
    , key : Key
    , page : Page
    , version : Version
    , listingModel : Page.Listing.Data.Model
    , detailModel : Page.Detail.Data.Model
    , createModel : Page.Create.Data.Model
    }


init : Key -> Url -> Flags -> Model
init key url flags =
    let
        page =
            Page.fromUrl url |> Maybe.withDefault ListingPage

        detailName =
            case page of
                DetailPage n ->
                    n

                _ ->
                    ""
    in
    { flags = flags
    , key = key
    , page = page
    , version = Data.Version.empty
    , listingModel = Page.Listing.Data.emptyModel
    , detailModel = Page.Detail.Data.emptyModel flags.apiBase detailName
    , createModel = Page.Create.Data.emptyModel flags
    }


type Msg
    = NavRequest UrlRequest
    | NavChange Url
    | VersionResp (Result Http.Error Version)
    | AllScripts (Result Http.Error (List ScriptInfo))
    | ListingMsg Page.Listing.Data.Msg
    | DetailMsg Page.Detail.Data.Msg
    | CreateMsg Page.Create.Data.Msg
    | SetPage Page
