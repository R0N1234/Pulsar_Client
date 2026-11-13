#!/usr/bin/env bash
# Versão anterior do mod do Pulsar, para a aba "Versões" do launcher (jogar a atual ou a anterior).
# Pega o jar da última versão lançada que não seja a atual: primeiro do Release; se não tiver, compila pela tag.
# Uso: anterior.sh <versão atual>   (precisa de GH_TOKEN)
set -euo pipefail
ATUAL="${1#v}"
DEST="$GITHUB_WORKSPACE/mod/build/anterior"
mkdir -p "$DEST"
TAG=$(gh release list --limit 30 --json tagName,isDraft,isPrerelease \
  -q "[.[] | select((.isDraft or .isPrerelease) | not) | .tagName | select(. != \"v$ATUAL\")][0] // \"\"")
if [ -z "$TAG" ]; then echo "Nenhuma versão anterior lançada."; exit 0; fi
echo "Versão anterior: $TAG"
# Até a 0.7.2 o jar saía sempre como pulsar-client-0.6.0.jar: o nome passa a ser o da versão da tag.
# Um Release publicado de novo pode ter sobrado o jar da versão de antes: fica só o que tem o nome da tag.
renomear() {
  local certo="$DEST/pulsar-client-${TAG#v}.jar" jar
  if [ -f "$certo" ]; then
    for jar in "$DEST"/pulsar-client-*.jar; do [ "$jar" = "$certo" ] || rm -f "$jar"; done
  else
    jar=$(ls "$DEST"/pulsar-client-*.jar | head -1)
    mv "$jar" "$certo"
  fi
  ls -la "$DEST"
}
if gh release download "$TAG" -p "pulsar-client-*.jar" -D "$DEST" --clobber 2>/dev/null && ls "$DEST"/pulsar-client-*.jar >/dev/null 2>&1; then
  find "$DEST" -name "*-sources.jar" -o -name "*-dev.jar" | xargs -r rm -f
  renomear; exit 0
fi
echo "O Release $TAG não tem o jar do mod; compilando pela tag."
git fetch -q --depth 1 origin "refs/tags/$TAG:refs/tags/$TAG"
rm -rf /tmp/pulsar-anterior
git worktree add -q /tmp/pulsar-anterior "$TAG"
(cd /tmp/pulsar-anterior/mod && chmod +x gradlew && ./gradlew build --no-daemon -q)
for f in /tmp/pulsar-anterior/mod/build/libs/pulsar-client-*.jar; do
  case "$f" in *-sources.jar|*-dev.jar) ;; *) cp "$f" "$DEST/" ;; esac
done
git worktree remove --force /tmp/pulsar-anterior
renomear
