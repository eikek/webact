module Data.Argument exposing (..)

import Data.Param exposing (Format(..), Param)
import File exposing (File)
import Http


type Argument
    = Str (Arg String)
    | Upl (Arg (List File))


type alias Arg a =
    { name : String
    , content : a
    , index : Int
    , input : Format
    }


makeTextArg : String -> String -> Int -> Argument
makeTextArg name content index =
    Str
        { name = name
        , content = content
        , index = index
        , input = Line
        }


makeFileArg : String -> File -> Int -> Argument
makeFileArg name content index =
    Upl
        { name = name
        , content = [ content ]
        , index = index
        , input = File
        }


makeEmptyArg : String -> Format -> Int -> Argument
makeEmptyArg name format index =
    case format of
        File ->
            Upl { name = name, content = [], index = index, input = File }

        Files ->
            Upl { name = name, content = [], index = index, input = Files }

        _ ->
            Str { name = name, content = "", index = index, input = format }


fromParam : Param -> Argument
fromParam param =
    makeEmptyArg param.name param.format 0


fold : (Arg String -> a) -> (Arg (List File) -> a) -> Argument -> a
fold f g arg =
    case arg of
        Str a ->
            f a

        Upl a ->
            g a


getIndex : Argument -> Int
getIndex arg =
    fold .index .index arg


getName : Argument -> String
getName arg =
    fold .name .name arg


getInput : Argument -> Format
getInput arg =
    fold .input .input arg


isInput : Argument -> Format -> Bool
isInput arg el =
    el == getInput arg


getText : Argument -> String
getText arg =
    fold .content (\_ -> "") arg


getFile : Argument -> List File
getFile arg =
    fold (\_ -> []) (\f -> f.content) arg


setIndex : Int -> Argument -> Argument
setIndex index arg =
    let
        set1 =
            \a -> Str { a | index = index }

        set2 =
            \a -> Upl { a | index = index }
    in
    fold set1 set2 arg


setInput : Format -> Argument -> Argument
setInput el arg =
    let
        trimList : List a -> List a
        trimList list =
            case list of
                [] ->
                    []

                x :: xs ->
                    [ x ]
    in
    case ( el, arg ) of
        ( File, Str a ) ->
            Upl { name = a.name, index = a.index, content = [], input = File }

        ( File, Upl a ) ->
            Upl { a | input = File, content = trimList a.content }

        ( Files, Str a ) ->
            Upl { name = a.name, index = a.index, content = [], input = Files }

        ( Files, Upl a ) ->
            Upl { a | input = Files }

        ( _, Str a ) ->
            Str { a | input = el }

        ( _, Upl a ) ->
            Str { name = a.name, index = a.index, content = "", input = el }


setName : String -> Argument -> Argument
setName name arg =
    let
        set1 =
            \a -> Str { a | name = name }

        set2 =
            \a -> Upl { a | name = name }
    in
    fold set1 set2 arg


setText : String -> Argument -> Argument
setText text arg =
    fold (\a -> Str { a | content = text }) (\a -> Upl a) arg


setFile : List File -> Argument -> Argument
setFile files arg =
    fold (\a -> Str a) (\a -> Upl { a | content = files }) arg


appendArg : Argument -> List Argument -> List Argument
appendArg arg list =
    let
        len =
            List.length list

        narg =
            setIndex len arg
    in
    list ++ [ narg ]


replaceArg : Argument -> List Argument -> List Argument
replaceArg arg list =
    let
        index =
            getIndex arg
    in
    List.map
        (\e ->
            if getIndex e == index then
                arg

            else
                e
        )
        list


removeArg : Argument -> List Argument -> List Argument
removeArg arg list =
    let
        index =
            getIndex arg
    in
    List.filter (\a -> index /= getIndex a) list


toPart : Argument -> List Http.Part
toPart arg =
    let
        name =
            getName arg
    in
    case arg of
        Str a ->
            [ Http.stringPart name a.content ]

        Upl a ->
            List.map (\f -> Http.filePart name f) a.content
