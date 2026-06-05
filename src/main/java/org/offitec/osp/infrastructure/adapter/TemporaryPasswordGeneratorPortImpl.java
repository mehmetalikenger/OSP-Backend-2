package org.offitec.osp.infrastructure.adapter;

import org.offitec.osp.domain.port.TemporaryPasswordGeneratorPort;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.stereotype.Component;

@Component
public class TemporaryPasswordGeneratorPortImpl implements TemporaryPasswordGeneratorPort {

    public String generate(){

        PasswordGenerator gen = new PasswordGenerator();

        CharacterRule lowerCaseRule =  new CharacterRule(EnglishCharacterData.LowerCase, 1);
        CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase, 1);
        CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit, 1);
        CharacterRule specialCharRule = new CharacterRule(EnglishCharacterData.Special, 1);

        return gen.generatePassword(8, lowerCaseRule, upperCaseRule, digitRule, specialCharRule);
    }
}
