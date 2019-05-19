module Api exposing (..)

import Process
import Task
import Http
import Json.Decode as Decode
import Data.ScriptInfo exposing (ScriptInfo)
import Data.Argument exposing (Argument)
import Data.RunningInfo exposing (RunningInfo)
import Data.Version exposing (Version)

versionInfo: ((Result Http.Error Version) -> msg) -> Cmd msg
versionInfo receive =
    Http.get
        { url = "/api/info/version"
        , expect = Http.expectJson receive Data.Version.versionDecoder
        }


fetchScripts: String -> ((Result Http.Error (List ScriptInfo)) -> msg) -> Cmd msg
fetchScripts baseurl receive =
    Http.get
        { url = (baseurl ++ "/scripts")
        , expect = Http.expectJson receive Data.ScriptInfo.scriptInfoListDecoder
        }

scriptDetail: String -> String -> ((Result Http.Error ScriptInfo) -> msg) -> Cmd msg
scriptDetail baseurl name receive =
    Http.get
        { url = baseurl ++ "/scripts/" ++ name
        , expect = Http.expectJson receive Data.ScriptInfo.scriptInfoDecoder
        }

scriptContent: String -> String -> ((Result Http.Error String) -> msg) -> Cmd msg
scriptContent baseurl name receive =
    Http.get
        { url = (baseurl ++ "/scripts/" ++ name ++ "/content")
        , expect = Http.expectString receive
        }

scriptStdout: String -> String -> ((Result Http.Error String) -> msg) -> Cmd msg
scriptStdout baseurl name receive =
    Http.get
        { url = (baseurl ++ "/scripts/" ++ name ++ "/output/stdout")
        , expect = Http.expectString receive
        }

scriptStderr: String -> String -> ((Result Http.Error String) -> msg) -> Cmd msg
scriptStderr baseurl name receive =
    Http.get
        { url = (baseurl ++ "/scripts/" ++ name ++ "/output/stderr")
        , expect = Http.expectString receive
        }

scriptUpload: String -> String -> String -> ((Result Http.Error ()) -> msg) -> Cmd msg
scriptUpload baseurl name content receive =
    Http.post
        { url = baseurl ++ "/scripts/" ++ name
        , body = Http.multipartBody
                 [ Http.stringPart "script" content
                 , Http.stringPart "message" "update"
                 ]
        , expect = Http.expectWhatever receive
        }

runScript: String -> String -> List Argument -> ((Result Http.Error ()) -> msg) -> Cmd msg
runScript baseurl name args receive =
    case args of
        [] ->
            Http.post
                { url = baseurl ++ "/scripts/" ++ name ++ "/run"
                , body = Http.emptyBody
                , expect = Http.expectWhatever receive
                }
        _ ->
            Http.post
                { url = baseurl ++ "/scripts/" ++ name ++ "/run"
                , expect = Http.expectWhatever receive
                , body = Http.multipartBody
                         (List.map Data.Argument.toPart args |> List.concat)
                }

runningState: String -> String -> ((Result Http.Error RunningInfo) -> msg) -> Cmd msg
runningState baseurl name receive =
    let
        decoder: Http.Response String -> Result Http.Error RunningInfo
        decoder resp =
            case resp of
                Http.BadUrl_ u -> (Err (Http.BadUrl u))
                Http.Timeout_ -> (Err Http.Timeout)
                Http.NetworkError_ -> (Err Http.NetworkError)
                Http.BadStatus_ meta body -> (Err (Http.BadStatus meta.statusCode))
                Http.GoodStatus_ meta body ->
                    case (Decode.decodeString Data.RunningInfo.runningInfoDecoder body) of
                        Err err -> Err (Http.BadBody (Decode.errorToString err))
                        Ok a -> Ok a
    in
    Process.sleep 800
        |> Task.andThen (\_ -> Http.task
                             { method = "GET"
                             , headers = []
                             , url = baseurl ++ "/scripts/" ++ name ++ "/running"
                             , body = Http.emptyBody
                             , resolver = Http.stringResolver decoder
                             , timeout = Nothing
                             }
                        )
        |> Task.attempt receive
