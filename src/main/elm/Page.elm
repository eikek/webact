module Page exposing
    ( Page(..)
    , fromUrl
    , goto
    , href
    , pageToString
    )

import Browser.Navigation as Nav
import Html exposing (Attribute)
import Html.Attributes as Attr
import Url exposing (Url)
import Url.Parser as Parser exposing ((</>), Parser, oneOf, s, string)


type Page
    = ListingPage
    | DetailPage String
    | CreatePage


pageToString : Page -> String
pageToString page =
    case page of
        ListingPage ->
            "#/list"

        DetailPage name ->
            "#/show/" ++ name

        CreatePage ->
            "#/create"


href : Page -> Attribute msg
href page =
    Attr.href (pageToString page)


parser : Parser (Page -> a) a
parser =
    oneOf
        [ Parser.map ListingPage Parser.top
        , Parser.map ListingPage (s "list")
        , Parser.map DetailPage (s "show" </> string)
        , Parser.map CreatePage (s "create")
        ]


fromUrl : Url -> Maybe Page
fromUrl url =
    { url | path = Maybe.withDefault "" url.fragment, fragment = Nothing }
        |> Parser.parse parser


goto : Page -> Cmd msg
goto page =
    Nav.load (pageToString page)
