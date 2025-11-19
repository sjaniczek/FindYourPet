import {onRequest} from "firebase-functions/https";
import * as logger from "firebase-functions/logger";

export const helloLog = onRequest((req, res) => {
  logger.info("Odpalono funkcję helloLog");
  res.send("Działa z loggerem!");
});