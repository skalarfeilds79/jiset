import fs from "fs";
import path from "path";
import { get } from "request-promise";
import { ECMAScriptVersion, HTMLTagAttribute } from "./enum";
import { ExtractorRule } from "./rule";

const fsOption = { encoding: "utf8" };

export const printSep = () => {
  console.log("==========================================================");
}

const getVersionNumber = (version: ECMAScriptVersion) => {
  switch (version) {
    case ECMAScriptVersion.es2020: return "11.0";
    case ECMAScriptVersion.es2019: return "10.0";
    case ECMAScriptVersion.es2018: return "9.0";
    case ECMAScriptVersion.es2017: return "8.0";
    case ECMAScriptVersion.es2016: return "7.0";
  }
}

const getSpecUrl = (version: ECMAScriptVersion) => {
  return `https://www.ecma-international.org/ecma-262/${ getVersionNumber(version) }/index.html`;
}

// get directory path and recursively create the non-existed directories in the path
export const getDir =
  (...args: string[]): string => {
    const dirPath = path.join.apply(path, args);
    if (!fs.existsSync(dirPath)) {
      fs.mkdirSync(dirPath);
    }
    return dirPath;
  }

// load specification
export const loadSpec = async (resourcePath: string, version: ECMAScriptVersion) => {
  printSep();
  console.log("loading spec...");

  /* check if cache exists */
  const cacheDirPath = getDir(resourcePath, ".cache");
  const cachePath = path.join(cacheDirPath, `${ version }.html`);
  const cacheExists = fs.existsSync(cachePath);
  let specContent: string;

  if (cacheExists) {
    /* if cache exists, read cached file */
    specContent = fs.readFileSync(cachePath, fsOption);
  } else {
    /* if cache doesn't exist, download spec from internet */
    const specUrl = getSpecUrl(version);
    console.log(`download ${ version } from ${ specUrl }...`);
    const HTMLContent: string = await get(specUrl);
    console.log(`completed`);
    /* save it to cache dir */
    fs.writeFileSync(cachePath, HTMLContent, fsOption);
    specContent = HTMLContent;
  }

  console.log("completed!!!");
  return specContent;
}

//load rules
export const loadRule =
  (resourcePath: string, version: ECMAScriptVersion | "eval"): ExtractorRule => {
    printSep();
    console.log("loading rules...");

    const rulePath = path.join(resourcePath, "rules", `${ version }.json`);
    const ruleExists = fs.existsSync(rulePath);

    /* assert rule file exists */
    if (!ruleExists)
      throw new Error(`loadRule: rulePath(${ rulePath }) is invalid`);

    const ruleContent = fs.readFileSync(rulePath, fsOption);
    const ruleObj = JSON.parse(ruleContent);

    console.log("completed!!!");
    return ruleObj;
  }

// save file
export const saveFile =
  (filePath: string, content: any) => {
    const str = JSON.stringify(content);
    fs.writeFileSync(filePath, str, fsOption);
  }

// get attributes from CheerioElement
export const getAllAttributes =
  (elem: CheerioElement): { [ attr: string ]: any } => {
    let ret: { [ attr: string ]: any } = {};
    // parse 
    for (let key in elem.attribs) {
      let val = elem.attribs[ key ];
      switch (key) {
        case HTMLTagAttribute.PARAMS: {
          ret[ key ] = val.split(",").map(_ => _.trim());
          break;
        }
        case HTMLTagAttribute.OPTIONAL: {
          ret[ key ] = true;
          break;
        }
        case HTMLTagAttribute.CONSTRAINTS: {
          switch (val[ 0 ]) {
            case "+":
              ret[ key ] = `p${ val.substr(1) }`; break;
            case "~":
              ret[ key ] = `!p${ val.substr(1) }`; break;
          }
          break;
        }
        case HTMLTagAttribute.ONEOF: {
          ret[ key ] = true;
          break;
        }
        default:
          ret[ key ] = val;
      }
    }

    // set params to array
    ret.params = ret.params ? ret.params : [];
    // set optional to boolean
    ret.optional = ret.optional ? ret.optional : false;
    // set constraints to string
    ret.constraints = ret.constraints ? ret.constraints : "";
    // set oneof to boolean
    ret.oneof = ret.oneof ? ret.oneof : false;

    return ret;
  }

// string normalization
export const norm = (str: string) => {
  return str
    .replace(/\s+/g, '')
    .replace(/\//g, '')
    .replace('#', '');
}

// simple string normalization
export const simpleNorm = (str:string) => {
  return str.replace(/[^a-zA-Z0-9.:(),]/g,'');
}

// normalize algorithm name
export const normName = (str: string) => {
  return str
    .replace(/%[^%]+%/g, x => 'INTRINSIC_' + x.substring(1, x.length-1))
    .replace(/\[@@[^%]+\]/g, x => '.SYMBOL_' + x.substring(3, x.length-1));
}

// spilt string
export const splitText = (str: string | undefined): string[] => {
  str = str || "";
  const tokens: string[] = [];
  let prevWordChar = false;
  for (let ch of str) {
    let isWordChar = /\w/.test(ch);
    let isSpace = /\s/.test(ch);
    if (prevWordChar && isWordChar) tokens.push(tokens.pop() + ch);
    else if (!isSpace) tokens.push(ch);
    prevWordChar = isWordChar;
  }
  return tokens;
}

// get ECMAScript Version
export const getESVersion = (target: string): ECMAScriptVersion => {
  switch (target) {
    case 'es11': return ECMAScriptVersion.es2020;
    case 'es10': return ECMAScriptVersion.es2019;
    case 'es9': return ECMAScriptVersion.es2018;
    case 'es8': return ECMAScriptVersion.es2017;
    case 'es7': return ECMAScriptVersion.es2016;
  }
  throw new Error(`getESVersion: Invalid ECMAScript version - ${target}`);
}

// copy values
export const copy = (given: any): any => {
  const target = JSON.parse(JSON.stringify(given));
  target.__proto__ = given.__proto__;
  return target;
}

// unwrapping characters
export const unwrap = (
  str: string,
  size: number = 1
): string => str.substring(size, str.length - size);