# Code Repository General License Agreement (2025 Revised)

**Version**: v1.2

**Last Updated**: July 9, 2025

**Copyright Notice** Copyright (c) 2025 饩雨/Mai_xiyu

Contact (Sole Universal Email): Mai_xiyu@vip.qq.com

All rights reserved.

---

## 1. Definitions and Interpretation

In this Agreement, the following terms shall have the meanings set out below:

### 1.1 Core Definitions

* **The Repository/The Software**: Refers to the source code repository hosted by the [Repository Maintainer] on platforms such as GitHub/GitLab/Gitee, including all source code, resource files, build artifacts (Jar/Zip, etc.), documentation, and related scripts contained therein.
* **Maintainer**: Refers to 饩雨/Mai_xiyu/God_xiyu and authorized collaborating developers.
* **User**: Refers to any entity that downloads, copies, installs, runs, modifies, or distributes the content of this Repository in any manner.

### 1.2 Definition of Work Types (Critical Distinction)

* **Derivative Works**: Refers to new works formed by modifying, translating, refactoring, porting, or directly copying code fragments based on the source code of this Repository. This includes, but is not limited to: Forked repositories, modified versions of the mod, and modpack cores that contain the code of this Repository.
* **Dependent Works (Affiliated/Linked Works)**: Refers to independent works that **DO NOT contain** the source code of this Repository but interact with, call upon, or extend the functionality of this Repository solely through APIs, Event Systems, Reflection, or Datapacks. Examples include: Add-on mods, integration plugins, and mods that rely on this mod as a prerequisite dependency.

---

## 2. Licensing Stratification Strategy

### 2.1 License for Derivative Works (Viral Restrictive License)

Any project deemed a **Derivative Work** must strictly adhere to **all terms of this Agreement** (specifically restrictions in Sections 3, 4, and 5).

* **Prohibition on License Change**: Derivative Works must not be changed to a more permissive open-source license (such as MIT, Apache 2.0, etc.).
* **Inheritance**: When distributing a Derivative Work, the full text of this Agreement must be included in a prominent location.

### 2.2 License for Dependent/Linked Works (Open Source & Autonomy)

Any software work published with this Repository as a **Prerequisite Dependency** or as an **Add-on Mod**:

1. **Open Source Requirement**: Must be open-sourced under the **GNU General Public License v3.0 (GNU GPL v3.0)** or any later version.
2. **Distribution Autonomy**: Subject to compliance with GPL v3.0, the author of the Dependent Work enjoys independent distribution rights (see Section 3.2.1 for exemptions).

---

## 3. Strict Usage Restrictions

### 3.1 Non-Commercial Use

Users shall not use the content of this Repository (including Derivative Works) for any commercial purpose.
**Prohibited acts include, but are not limited to:**

1. **Paid Products**: Embedding the code into paid software, server cores, login authenticators, or hardware devices.
2. **For-Profit Services**: Using this Software as an exclusive feature or selling point for fee-based servers (Pay-to-Win or Membership-based).
3. **Direct/Indirect Profit**: Selling download links for this Software, using this Software to generate advertising traffic, or using it as a reward for crowdfunding/sponsorships.
4. **Resale and Licensing**: Selling, renting, or sub-licensing any part of this Software is prohibited.

### 3.2 Prohibition on Specific Platforms (Anti-Platform)

To prevent unauthorized integration and misuse, it is **strictly prohibited** to publish, upload, or distribute the content of this Repository (including Derivative Works and Build Artifacts) to any platform operated or controlled by **NetEase (Inc.) and its affiliates**.
**The prohibited scope explicitly includes:**

1. Minecraft China Edition (including Rental Servers, Network Game Halls, and Component Centers).
2. NetEase Yunxin, NetEase Game Developer Platform.
3. Third-party mod hosting platforms invested in or controlled by NetEase (if any).

#### 3.2.1 Exemption for Dependent/Linked Works (New Clause)

**The restrictions in Section 3.2 DO NOT apply to "Dependent/Linked Works".**
The platform for publishing Dependent or Linked Mods developed by third parties is decided solely by the author of said work. **Authors of Dependent Mods have the right to publish their developed Dependent Mods to any platform, including NetEase**, without obtaining consent from the Repository Maintainer.
*(Note: This exemption applies ONLY to the code and resources of the Dependent Mod itself. If the Dependent Mod packages the original files of this Repository for joint distribution, such behavior remains a violation.)*

### 3.3 Integrity and Attribution

1. **Prohibition on Metadata Tampering**: Users must not delete or modify author information (Authors), license declarations (License), or links in files such as `mcmod.info`, `fabric.mod.json`, or `mods.toml`.
2. **Fork Restrictions**: Forked repositories must retain links to the original repository and this Copyright Notice in a prominent position at the top of the README file.

---

## 4. Contributions and Rights

1. **Contributor Authorization**: Contributors agree that code submitted via Pull Request or Commit is automatically authorized to the Maintainer for management. The Maintainer has the right to adjust the license or refactor the code in future versions without seeking further consent from the contributor.
2. **Infringement Liability**: Contributors must ensure that submitted content does not infringe upon the intellectual property rights of any third party. If a legal dispute arises due to infringing content submitted by a contributor, the contributor shall bear full responsibility.

---

## 5. Breach of Contract and Termination

1. **Automatic Termination**: If a User violates any term of this Agreement (especially Commercial Use or NetEase-related clauses), this authorization terminates automatically and immediately.
2. **Post-Termination Obligations**: Upon termination, the User must immediately stop distributing this Software and destroy all copies held (including local storage, server data, Forked repositories, etc.).
3. **Blacklist Mechanism**: The Maintainer reserves the right to publish a list of violators (including IDs and related project links) and to limit the violator's use of this Software through technical means (e.g., within the code logic).

---

## 6. Disclaimer

THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED.
TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, THE MAINTAINER SHALL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES (INCLUDING BUT NOT LIMITED TO LOSS OF DATA, COMPUTER FAILURE, SERVER DOWNTIME, OR BUSINESS INTERRUPTION) ARISING OUT OF THE USE OF THIS SOFTWARE.
**Risk Warning**: Users assume all risks associated with running experimental code.

---

## 7. Governing Law and Dispute Resolution

1. This Agreement shall be governed by the **laws of the People's Republic of China**.
2. In the event of a dispute, both parties shall prioritize amicable negotiation; if negotiation fails, the dispute shall be subject to the jurisdiction of the People's Court located at the **location of the Maintainer**.
