

```bash
mvn -DskipTests=true clean compile && mvn -DskipTests=false test || true; failed_files=(); for f in target/surefire-reports/*.xml; do grep -E '<testsuite [^>]*(errors="([1-9][0-9]*)"|failures="([1-9][0-9]*)")' "$f" >/dev/null && failed_files+=("$f"); done; if [ ${#failed_files[@]} -eq 0 ]; then echo 'No failing surefire reports found in target/surefire-reports'; else for f in "${failed_files[@]}"; do echo "---- $f ----"; sed -n '1,240p' "$f"; txt="${f%.xml}.txt"; [ -f "$txt" ] && { echo "---- $txt ----"; sed -n '1,240p' "$txt"; }; done; fi

```