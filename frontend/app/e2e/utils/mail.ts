import { join } from 'path';
import { readFileSync } from 'fs';

const linkPattern = /.*href="(http:\/\/.*)".*$/;

export const findLinkFromDockerLog = () => {
  const dockerLogFile = join(process.cwd(), '..', '..', 'docker.log');
  const lines = String(readFileSync(dockerLogFile)).split('\n');

  return lines.reduce((maybeLink, line) => {
    const match = linkPattern.exec(line);
    if (match) {
      return match[1];
    }
    return maybeLink;
  }, undefined as string | undefined);
};
