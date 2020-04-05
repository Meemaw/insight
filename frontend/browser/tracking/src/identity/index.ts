/* eslint-disable no-console */
/* eslint-disable lodash/prefer-lodash-typecheck */
/* eslint-disable no-restricted-globals */
/* eslint-disable @typescript-eslint/camelcase */
/* eslint-disable no-underscore-dangle */

import { PageResponse } from 'backend/types';

type InsightIdentity = {
  orgId: string;
  uid: string;
  sessionId: string;
  host: string;
  expiresSeconds: number;
};

type Cookie = Partial<InsightIdentity>;

const storageKey = '_is_uid';

class Identity {
  private readonly _cookie: Cookie;

  constructor(cookie: Cookie) {
    this._cookie = cookie;
  }

  public static initFromCookie = (host: string, orgId: string) => {
    const cookies = document.cookie.split(';').reduce((acc, value) => {
      const valueSplit = value.split('=');
      return { ...acc, [valueSplit[0]]: valueSplit[1] };
    }, {} as { _is_uid?: string });

    console.debug('[initFromCookie]', { cookies, host, orgId });
    let maybeCookie = cookies[storageKey];
    if (!maybeCookie) {
      try {
        maybeCookie = localStorage[storageKey];
        console.debug('Restored identity from localStorage', maybeCookie);
      } catch (err) {
        // noop
      }
    } else {
      console.debug('Restored identity from cookie', maybeCookie);
    }

    const decoded = Identity.decodeIdentity(maybeCookie);
    if (decoded) {
      if (decoded.orgId === orgId) {
        console.debug('Matching orgId, setting identity', decoded);
        return new Identity(decoded);
      }
      console.debug('Unmatching identity', { decoded, orgId });
    } else {
      console.debug('Could not parse identity');
    }

    const newIdentity = {
      expiresSeconds: Identity.createExpiresSeconds(),
      host,
      orgId,
      uid: '',
      sessionId: '',
    };

    console.debug('Created new identity', newIdentity);
    return new Identity(newIdentity);
  };

  private encode = (expirationAbsTimeSeconds: number) => {
    return `${this._cookie.host}#${this._cookie.orgId}#${this._cookie.uid}:${this._cookie.sessionId}/${expirationAbsTimeSeconds}`;
  };

  public handleIdentity = (pageResponse: PageResponse) => {
    this._cookie.uid = pageResponse.data.uid;
    this._cookie.sessionId = pageResponse.data.sessionId;
    this.writeIdentity();
  };

  private writeIdentity = () => {
    const expirationAbsTimeSeconds = this._cookie.expiresSeconds as number;
    const encoded = this.encode(expirationAbsTimeSeconds);
    const expires = new Date(1e3 * expirationAbsTimeSeconds).toUTCString();
    this.setCookie(encoded, expires);
    try {
      localStorage[storageKey] = encoded;
    } catch (e) {
      // noop
    }
    console.debug('Wrote identity', encoded);
  };

  private setCookie = (encoded: string, expires: string) => {
    let cookie = `${storageKey}=${encoded}; domain=; Expires=${expires}; path=/; SameSite=Strict`;
    if (location.protocol === 'https:') {
      cookie += '; Secure';
    }
    document.cookie = cookie;
  };

  private static decodeIdentity = (
    encoded: string | undefined
  ): InsightIdentity | undefined => {
    if (!encoded) {
      return undefined;
    }
    const [maybeIdentity, maybeExpiresSeconds] = encoded.split('/');
    const expiresSeconds = parseInt(maybeExpiresSeconds, 10);
    if (isNaN(expiresSeconds) || expiresSeconds < Identity.currentMillis()) {
      return undefined;
    }

    const identitySplit = maybeIdentity.split(/[#,]/);
    if (identitySplit.length !== 3) {
      return undefined;
    }

    const [uid, sessionId] = identitySplit[2].split(':');

    return {
      uid,
      sessionId,
      host: identitySplit[0],
      orgId: identitySplit[1],
      expiresSeconds,
    };
  };

  private static currentMillis = () => {
    return Math.floor(Date.now() / 1e3);
  };

  private static createExpiresSeconds = () => {
    return Identity.currentMillis() + 31536e3;
  };
}

export default Identity;